package com.gitbitex.feed.message;

import java.util.Collection;
import java.util.Date;

import com.gitbitex.matchingengine.snapshot.L2OrderBookUpdate;
import com.gitbitex.matchingengine.snapshot.L2PageLineChange;
import lombok.Getter;
import lombok.Setter;

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
public class L2UpdateMessage {
    private String productId;
    private String time;
    private Collection<L2PageLineChange> changes;

    public L2UpdateMessage() {
    }

    public L2UpdateMessage(L2OrderBookUpdate l2OrderBookUpdate) {
        this.productId = "l2update";
        this.productId = l2OrderBookUpdate.getProductId();
        this.changes = l2OrderBookUpdate.getChanges();
        this.time = new Date().toInstant().toString();
    }
}
