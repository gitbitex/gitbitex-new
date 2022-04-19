package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class L2SnapshotMessage extends L2OrderBook {
    private String type = "snapshot";

    public L2SnapshotMessage(L2OrderBook snapshot) {
        BeanUtils.copyProperties(snapshot, this);
    }
}
