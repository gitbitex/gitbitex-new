package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class L2OrderBookMessage extends L2OrderBook {
    private String type;

    public L2OrderBookMessage(L2OrderBook snapshot) {
        this.type = "snapshot";
        BeanUtils.copyProperties(snapshot, this);
    }
}
