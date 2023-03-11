package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.L2OrderBook;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class L2SnapshotFeedMessage extends L2OrderBook {
    private String type = "snapshot";

    public L2SnapshotFeedMessage(L2OrderBook snapshot) {
        BeanUtils.copyProperties(snapshot, this);
    }
}
