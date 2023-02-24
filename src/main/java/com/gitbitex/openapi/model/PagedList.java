package com.gitbitex.openapi.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
