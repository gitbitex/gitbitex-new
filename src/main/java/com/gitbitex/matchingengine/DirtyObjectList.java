package com.gitbitex.matchingengine;

import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class DirtyObjectList<T> extends ArrayList<T> {
    @Getter
    private final AtomicLong flushedCount = new AtomicLong();

    public static <T> DirtyObjectList<T> singletonList(T obj) {
        DirtyObjectList<T> list = new DirtyObjectList<>();
        list.add(obj);
        return list;
    }

    public boolean isAllFlushed() {
        return flushedCount.get() == size();
    }

    @Override
    public boolean add(T o){
        if (o==null){
            throw new NullPointerException("o");
        }
        return super. add(o);
    }
}

