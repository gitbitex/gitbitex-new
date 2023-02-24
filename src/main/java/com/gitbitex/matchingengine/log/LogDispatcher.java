package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LogDispatcher {

    public static void dispatch(Log orderMessage,LogHandler handler) {
         if (orderMessage instanceof OrderRejectedMessage) {
            handler.on((OrderRejectedMessage)orderMessage);
        } else if (orderMessage instanceof OrderFilledMessage) {
            handler.on((OrderFilledMessage)orderMessage);
        } else if (orderMessage instanceof OrderOpenMessage) {
            handler.on((OrderOpenMessage)orderMessage);
        } else if (orderMessage instanceof OrderReceivedMessage) {
            handler.on((OrderReceivedMessage)orderMessage);
        } else if (orderMessage instanceof OrderDoneMessage) {
            handler.on((OrderDoneMessage)orderMessage);
        } else if (orderMessage instanceof OrderMatchLog) {
            handler.on((OrderMatchLog)orderMessage);

        } else if (orderMessage instanceof AccountChangeMessage) {
            handler.on((AccountChangeMessage)orderMessage);




        } else {
            logger.warn("Unhandled command: {} {}", orderMessage.getClass().getName(), JSON.toJSONString(orderMessage));
        }
    }
}
