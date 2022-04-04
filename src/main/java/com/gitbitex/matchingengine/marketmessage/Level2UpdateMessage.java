package com.gitbitex.matchingengine.marketmessage;

import com.gitbitex.matchingengine.PageLine;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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
public class Level2UpdateMessage extends MarketMessage {
    private String time;
    private List<List<String>> changes;

    public Level2UpdateMessage() {

    }

    public Level2UpdateMessage(String productId, List<PageLine> lines) {
        this.setType("l2update");
        this.setProductId(productId);
        this.time = new Date().toInstant().toString();
        this.changes = new ArrayList<>();

        Map<String, PageLine> lineByKey = new HashMap<>();
        for (PageLine line : lines) {
            lineByKey.put(line.getSide().name() + line.getPrice().stripTrailingZeros().toPlainString(), line);
        }

        lineByKey.values().forEach(x -> {
            List<String> change = new ArrayList<>(3);
            change.add(x.getSide().name().toLowerCase());
            change.add(x.getPrice().stripTrailingZeros().toPlainString());
            change.add(x.getTotalSize().stripTrailingZeros().toPlainString());
            this.changes.add(change);
        });
    }

}
