package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.snapshot.L2OrderBookSnapshot;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class L2OrderBookSnapshotMessage extends L2OrderBookSnapshot {
    private String type;

    public L2OrderBookSnapshotMessage(L2OrderBookSnapshot snapshot) {
        this.type = "snapshot";
        BeanUtils.copyProperties(snapshot, this);
    }
}
