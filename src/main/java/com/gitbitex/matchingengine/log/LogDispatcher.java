package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LogDispatcher {

    public static void dispatch(Log orderMessage,LogHandler handler) {
         if (orderMessage instanceof OrderRejectedLog) {
            handler.on((OrderRejectedLog)orderMessage);
        } else if (orderMessage instanceof OrderFilledMessage) {
            handler.on((OrderFilledMessage)orderMessage);
        } else if (orderMessage instanceof OrderOpenLog) {
            handler.on((OrderOpenLog)orderMessage);
        } else if (orderMessage instanceof OrderReceivedLog) {
            handler.on((OrderReceivedLog)orderMessage);
        } else if (orderMessage instanceof OrderDoneLog) {
            handler.on((OrderDoneLog)orderMessage);
        } else if (orderMessage instanceof OrderMatchLog) {
            handler.on((OrderMatchLog)orderMessage);

        } else if (orderMessage instanceof AccountChangeLog) {
            handler.on((AccountChangeLog)orderMessage);




        } else {
            logger.warn("Unhandled command: {} {}", orderMessage.getClass().getName(), JSON.toJSONString(orderMessage));
        }
    }
}
