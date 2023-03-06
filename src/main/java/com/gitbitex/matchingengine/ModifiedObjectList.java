package com.gitbitex.matchingengine;

import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ModifiedObjectList<T> extends ArrayList<T> {
    @Getter
    private final AtomicLong savedCount = new AtomicLong();

    public static <T> ModifiedObjectList<T> singletonList(T obj) {
        ModifiedObjectList<T> list = new ModifiedObjectList<>();
        list.add(obj);
        return list;
    }

    public boolean isAllSaved() {
        return savedCount.get() == size();
    }

    @Override
    public boolean add(T o){
        if (o==null){
            throw new NullPointerException("o");
        }
        return super. add(o);
    }
}

