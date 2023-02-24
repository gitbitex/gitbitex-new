package com.gitbitex.common.message;

import com.gitbitex.marketdata.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPlacedMessage extends OrderMessage {
    private Order order;

    public OrderPlacedMessage() {
        this.setType(CommandType.ORDER_PLACED);
    }
}
