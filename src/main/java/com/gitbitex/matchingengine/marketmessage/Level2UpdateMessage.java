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
public class Level2UpdateMessage extends MarketMessage {
    private String time;
    private List<Level2UpdateLine> changes;

    public Level2UpdateMessage() {
    }

    public Level2UpdateMessage(String productId, List<PageLine> lines) {
        this.setType("l2update");
        this.setProductId(productId);
        this.time = new Date().toInstant().toString();
        this.changes = new ArrayList<>();
        this.changes = lines.stream().map(Level2UpdateLine::new).collect(Collectors.toList());
    }

    public static class Level2UpdateLine extends ArrayList<Object> {
        public Level2UpdateLine() {
        }

        public Level2UpdateLine(PageLine line) {
            this.add(line.getSide().name().toLowerCase());
            this.add(line.getPrice().stripTrailingZeros().toPlainString());
            this.add(line.getTotalSize().stripTrailingZeros().toPlainString());
        }
    }

}
