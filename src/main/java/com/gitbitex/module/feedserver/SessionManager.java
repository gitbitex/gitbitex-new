package com.gitbitex.module.feedserver;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.alibaba.fastjson.JSON;

import com.gitbitex.module.feedserver.message.PongMessage;
import com.gitbitex.module.matchingengine.OrderBookSnapshotManager;
import com.gitbitex.module.matchingengine.TickerManager;
import com.gitbitex.module.matchingengine.marketmessage.Level2OrderBookSnapshot;
import com.gitbitex.module.matchingengine.marketmessage.TickerMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionManager {
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> sessionIdsByChannel
        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> channelsBySessionId
        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> sessionById = new ConcurrentHashMap<>();
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final TickerManager tickerManager;

    @SneakyThrows
    public void subOrUnSub(WebSocketSession session, List<String> productIds, List<String> currencies,
        List<String> channels, boolean isSub) {
        for (String channel : channels) {
            switch (channel) {
                case "level2":
                    for (String productId : productIds) {
                        String productChannel = productId + "." + channel;

                        if (isSub) {
                            subscribeChannel(session, productChannel);

                            try {
                                Level2OrderBookSnapshot snapshot = orderBookSnapshotManager.getLevel2BookSnapshot(
                                    productId);
                                if (snapshot != null) {
                                    session.sendMessage(new TextMessage(JSON.toJSONString(snapshot)));
                                }
                            } catch (Exception e) {
                                logger.error("send level2 snapshot error: {}", e.getMessage(), e);
                            }
                        } else {
                            unsubscribeChannel(session, productChannel);
                        }
                    }
                    break;
                case "ticker":
                    for (String productId : productIds) {
                        String productChannel = productId + "." + channel;

                        if (isSub) {
                            subscribeChannel(session, productChannel);

                            try {
                                TickerMessage tickerMessage = tickerManager.getTicker(productId);
                                if (tickerMessage != null) {
                                    session.sendMessage(new TextMessage(JSON.toJSONString(tickerMessage)));
                                }
                            } catch (Exception e) {
                                logger.error("send ticker error: {}", e.getMessage(), e);
                            }
                        } else {
                            unsubscribeChannel(session, productChannel);
                        }
                    }
                    break;
                case "match":
                    for (String productId : productIds) {
                        String productChannel = productId + "." + channel;
                        if (isSub) {
                            subscribeChannel(session, productChannel);
                        } else {
                            unsubscribeChannel(session, productChannel);
                        }
                    }
                    break;
                case "order": {
                    String userId = getUserId(session);
                    if (userId == null) {
                        logger.error("no userid");
                        return;
                    }

                    for (String productId : productIds) {
                        String orderChanel = userId + "." + productId + "." + channel;
                        if (isSub) {
                            subscribeChannel(session, orderChanel);
                        } else {
                            unsubscribeChannel(session, orderChanel);
                        }
                    }
                    break;
                }
                case "funds": {
                    String userId = getUserId(session);
                    if (userId == null) {
                        logger.error("no userid");
                        return;
                    }

                    if (currencies != null) {
                        for (String currency : currencies) {
                            String accountChannel = userId + "." + currency + "." + channel;
                            if (isSub) {
                                subscribeChannel(session, accountChannel);
                            } else {
                                unsubscribeChannel(session, accountChannel);
                            }
                        }
                    }

                    break;
                }

                default:
            }
        }
    }

    public void sendMessageToChannel(String channel, String message) {
        Set<String> sessionIds = sessionIdsByChannel.get(channel);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        sessionIds.parallelStream().forEach(sessionId -> {
            try {
                WebSocketSession session = sessionById.get(sessionId);
                if (session != null) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (Exception e) {
                logger.error("send error: {}", e.getMessage(), e);
            }
        });
    }

    public void sendPong(WebSocketSession session) {
        try {
            PongMessage pongMessage = new PongMessage();
            pongMessage.setType("pong");
            session.sendMessage(new TextMessage(JSON.toJSONString(pongMessage)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subscribeChannel(WebSocketSession session, String channel) {
        logger.info("sub: {} {}", session.getId(), channel);
        sessionIdsByChannel
            .computeIfAbsent(channel, k -> new ConcurrentSkipListSet<>())
            .add(session.getId());
        channelsBySessionId.computeIfAbsent(session.getId(), k -> new ConcurrentSkipListSet<>())
            .add(channel);
        sessionById.put(session.getId(), session);
    }

    public void unsubscribeChannel(WebSocketSession session, String channel) {
        if (sessionIdsByChannel.containsKey(channel)) {
            sessionIdsByChannel.get(channel).remove(session.getId());
        }
        channelsBySessionId.computeIfPresent(session.getId(), (k, v) -> {
            v.remove(channel);
            return v;
        });
    }

    public void removeSession(WebSocketSession session) {
        ConcurrentSkipListSet<String> channels = channelsBySessionId.remove(session.getId());
        if (channels != null) {
            for (String channel : channels) {
                ConcurrentSkipListSet<String> sessionIds = sessionIdsByChannel.get(channel);
                if (sessionIds != null) {
                    sessionIds.remove(session.getId());
                }
            }
        }
        sessionById.remove(session.getId());
    }

    public String getUserId(WebSocketSession session) {
        Object val = session.getAttributes().get("CURRENT_USER_ID");
        return val != null ? val.toString() : null;
    }

}
