package com.gitbitex.order.command;

import java.math.BigDecimal;

import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FillOrderCommand extends OrderCommand {
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private String productId;
    private long tradeId;
    private Order.OrderSide side;
    private String fillId;

    public FillOrderCommand() {
        this.setType(Typ.FILL_ORDER);
    }
}
