package com.gitbitex.feed;

import com.alibaba.fastjson.JSON;
import com.gitbitex.feed.message.L2SnapshotMessage;
import com.gitbitex.feed.message.L2UpdateMessage;
import com.gitbitex.feed.message.PongMessage;
import com.gitbitex.feed.message.TickerMessage;
import com.gitbitex.marketdata.TickerManager;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.L2OrderBookChange;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionManager {
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> sessionIdsByChannel
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<String>> channelsBySessionId
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> sessionById = new ConcurrentHashMap<>();
    private final OrderBookManager orderBookManager;
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
                                L2OrderBook l2OrderBook = orderBookManager.getL2BatchOrderBook(productId);
                                if (l2OrderBook != null) {
                                    sendL2OrderBook(session, l2OrderBook);
                                }
                            } catch (Exception e) {
                                logger.error("send level2 snapshot error: {}", e.getMessage(), e);
                            }
                        } else {
                            String key = "LAST_L2_ORDER_BOOK:" + productId;
                            session.getAttributes().remove(key);
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
                                Ticker ticker = tickerManager.getTicker(productId);
                                if (ticker != null) {
                                    sendJson(session, new TickerMessage(ticker));
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

    public void sendMessageToChannel(String channel, Object message) {
        Set<String> sessionIds = sessionIdsByChannel.get(channel);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        sessionIds.parallelStream().forEach(sessionId -> {
            try {
                WebSocketSession session = sessionById.get(sessionId);
                if (session != null) {
                    synchronized (session) {
                        if (message instanceof L2OrderBook) {
                            sendL2OrderBook(session, (L2OrderBook) message);
                        } else {
                            sendJson(session, message);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("send error: {}", e.getMessage(), e);
            }
        });
    }

    private void sendL2OrderBook(WebSocketSession session, L2OrderBook l2OrderBook) throws IOException {
        String key = "LAST_L2_ORDER_BOOK:" + l2OrderBook.getProductId();

        if (!session.getAttributes().containsKey(key)) {
            sendJson(session, new L2SnapshotMessage(l2OrderBook));
            session.getAttributes().put(key, l2OrderBook);
            return;
        }

        L2OrderBook lastL2OrderBook = (L2OrderBook) session.getAttributes().get(key);
        if (lastL2OrderBook.getSequence() >= l2OrderBook.getSequence()) {
            logger.warn("discard l2 order book, too old: last={} new={}", lastL2OrderBook.getSequence(),
                    l2OrderBook.getSequence());
            return;
        }

        List<L2OrderBookChange> changes = lastL2OrderBook.diff(l2OrderBook);
        if (changes == null || changes.isEmpty()) {
            return;
        }
        L2UpdateMessage l2UpdateMessage = new L2UpdateMessage(l2OrderBook.getProductId(), changes);
        sendJson(session, l2UpdateMessage);
        session.getAttributes().put(key, l2OrderBook);
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

    private void sendJson(WebSocketSession session, Object msg) throws IOException {
        session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
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
        logger.info("unsub: {} {}", session.getId(), channel);
        if (sessionIdsByChannel.containsKey(channel)) {
            sessionIdsByChannel.get(channel).remove(session.getId());
        }
        channelsBySessionId.computeIfPresent(session.getId(), (k, v) -> {
            v.remove(channel);
            return v;
        });
        if (channelsBySessionId.containsKey(session.getId())) {
            logger.info("now: {} {}", session.getId(), JSON.toJSONString(channelsBySessionId.get(session.getId())));
        }
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
