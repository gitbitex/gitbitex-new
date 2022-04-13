package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;

@Getter
public class SlidingBloomFilter implements Serializable {
    private final List<BloomFilter<String>> filters;
    private final int expectedInsertions;
    private int idx;

    public SlidingBloomFilter(int expectedInsertions, int filterCount) {
        this.expectedInsertions = expectedInsertions;
        this.filters = new ArrayList<>(filterCount);
        for (int i = 0; i < filterCount; i++) {
            this.filters.add(null);
        }
    }

    public boolean contains(String orderId) {
        for (BloomFilter<String> filter : filters) {
            if (filter!=null) {
                if (filter.mightContain(orderId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void put(String orderId) {
        getCurrentFilter().put(orderId);
    }

    private BloomFilter<String> getCurrentFilter() {
        if (filters.get(idx) == null) {
            filters.set(idx, createBloomFilter());
        } else if (isFilterFull(filters.get(idx))) {
            ++idx;
            if (idx == filters.size()) {
                idx = 0;
            }
            if (filters.get(idx) == null || isFilterFull(filters.get(idx))) {
                filters.set(idx, createBloomFilter());
            }
        }
        return filters.get(idx);
    }

    private BloomFilter<String> createBloomFilter() {
        return BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, 0.00000001);
    }

    private boolean isFilterFull(BloomFilter<String> filter) {
        return filter.approximateElementCount() > expectedInsertions;
    }
}
