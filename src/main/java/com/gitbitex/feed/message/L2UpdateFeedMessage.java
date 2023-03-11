package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.L2OrderBookChange;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

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
public class L2UpdateFeedMessage {
    private String type = "l2update";
    private String productId;
    private String time;
    private List<L2OrderBookChange> changes;

    public L2UpdateFeedMessage() {
    }

    public L2UpdateFeedMessage(String productId, List<L2OrderBookChange> l2OrderBookChanges) {
        this.productId = productId;
        this.time = new Date().toInstant().toString();
        this.changes = l2OrderBookChanges;
    }
}
