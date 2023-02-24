package com.gitbitex.matchingengine.command;

import com.gitbitex.marketdata.entity.Order;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;

@Getter
@Setter
public class PlaceOrderCommand extends MatchingEngineCommand {
    private Order order;

    private String baseCurrency;
    private String quoteCurrency;

    public PlaceOrderCommand() {
        this.setType(CommandType.PLACE_ORDER);
    }
}
