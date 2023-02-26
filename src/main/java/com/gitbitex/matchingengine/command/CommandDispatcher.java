package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CommandDispatcher {

    public static void dispatch(MatchingEngineCommand orderMessage,MatchingEngineCommandHandler handler) {
        if (orderMessage instanceof PlaceOrderCommand) {
            handler.on((PlaceOrderCommand)orderMessage);
        } else if (orderMessage instanceof CancelOrderCommand) {
            handler.on((CancelOrderCommand)orderMessage);
        } else if (orderMessage instanceof DepositCommand) {
            handler.on((DepositCommand)orderMessage);
        } else {
            logger.warn("Unhandled command: {} {}", orderMessage.getClass().getName(), JSON.toJSONString(orderMessage));
        }
    }
}
