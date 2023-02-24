package com.gitbitex.common.message;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage;

public interface OrderMessageHandler {
    default void on(OrderPlacedMessage command) {}

    default void on(PlaceOrderCommand command) {}

    default void on(OrderRejectedMessage command) {}

    default void on(CancelOrderCommand command) {}

    default void on(OrderFilledMessage command) {}

    default void on(OrderReceivedMessage command) {}

    default void on(OrderDoneMessage command) {}

    default void on(OrderOpenMessage command) {}

    default void on(OrderMatchLog command) {}

    default void on(DepositCommand command){}

    default void on(AccountChangeMessage message){}

}
