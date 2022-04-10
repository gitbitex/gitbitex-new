package com.gitbitex.matchingengine.marketmessage;

import com.gitbitex.matchingengine.PageLine;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {
 * "type": "l2update",
 * "product_id": "BTC-USD",
 * "time": "2019-08-14T20:42:27.265Z",
 * "changes": [
 * [
 * "buy",
 * "10101.80000000",
 * "0.162567"
 * ]
 * ]
 * }
 */
@Getter
@Setter
public class L2UpdateMessage extends MarketMessage {
    private String time;
    private List<L2Change> changes;

    public L2UpdateMessage() {
    }

    public L2UpdateMessage(String productId, List<L2Change> changes) {
        this.setType("l2update");
        this.setProductId(productId);
        this.time = new Date().toInstant().toString();
        this.changes = changes;
    }

    public static class L2Change extends ArrayList<Object> {
        public L2Change() {
        }

        public L2Change(PageLine line) {
            this.add(line.getSide().name().toLowerCase());
            this.add(line.getPrice().stripTrailingZeros().toPlainString());
            this.add(line.getTotalSize().stripTrailingZeros().toPlainString());
        }
    }

}
