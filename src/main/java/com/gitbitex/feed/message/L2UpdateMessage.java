package com.gitbitex.feed.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.marketmessage.L2Change;
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
    private List<Change> changes;

    public L2UpdateMessage() {
    }

    public L2UpdateMessage(String productId, List<L2Change> changes) {
        this.productId = "l2update";
        this.setProductId(productId);
        this.time = new Date().toInstant().toString();
        this.changes = changes.stream().map(x->new Change(x)).collect(Collectors.toList());
    }

    public static class Change extends ArrayList<Object> {
        public Change() {
        }

        public Change(L2Change change) {
            this.add(change.getSide().name().toLowerCase());
            this.add(change.getPrice().stripTrailingZeros().toPlainString());
            this.add(change.getSize().stripTrailingZeros().toPlainString());
        }
    }

}
