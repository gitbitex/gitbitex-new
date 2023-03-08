package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;

public class ModifiedObjectList<T> extends ArrayList<T> {
    @Getter
    private final AtomicLong savedCount = new AtomicLong();
    @Getter
    private long commandOffset;

    public ModifiedObjectList(long commandOffset) {
        super();
        this.commandOffset = commandOffset;
    }

    public boolean isAllSaved() {
        return savedCount.get() == size();
    }

    @Override
    public boolean add(T o) {
        if (o == null) {
            throw new NullPointerException("o");
        }
        return super.add(o);
    }
}

