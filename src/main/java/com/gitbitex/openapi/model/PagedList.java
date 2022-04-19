package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PagedList<T> {
    private List<T> items;
    private long count;

    public PagedList(List<T> items, long count) {
        this.items = items;
        this.count = count;
    }
}
