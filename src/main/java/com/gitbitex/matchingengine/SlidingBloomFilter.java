package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SlidingBloomFilter {
    private List<BloomFilter<String>> filters;
    private int expectedInsertions;
    private int idx;

    public SlidingBloomFilter(int expectedInsertions, int filterCount) {
        this.expectedInsertions = expectedInsertions;
        this.filters = new ArrayList<>(filterCount);
        for (int i = 0; i < filterCount; i++) {
            this.filters.add(null);
        }
    }

    public SlidingBloomFilter(int expectedInsertions, int idx, List<BloomFilter<String>> filters) {
        this.expectedInsertions = expectedInsertions;
        this.idx = idx;
        this.filters = filters;
    }

    public boolean contains(String orderId) {
        for (BloomFilter<String> filter : filters) {
            if (filter.mightContain(orderId)) {
                return true;
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

    public SlidingBloomFilter copy() {
        SlidingBloomFilter slidingBloomFilter = new SlidingBloomFilter(1,111);
        slidingBloomFilter.idx = this.idx;
        for (BloomFilter<String> filter : this.filters) {
            if (filter!=null) {
                Object o = filter.copy();
                System.out.println(o.getClass());
            }
        }
        return slidingBloomFilter;
    }

    private BloomFilter<String> createBloomFilter() {
        return BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, 0.00000001);
    }

    private boolean isFilterFull(BloomFilter<String> filter) {
        return filter.approximateElementCount() > expectedInsertions ;
    }

    public static void main(String[] a) throws IOException {
        SlidingBloomFilter slidingBloomFilter = new SlidingBloomFilter(100000000, 3);



        for (int i = 0; i < 10000000; i++) {
            slidingBloomFilter.put(String.valueOf(i));
        }

        for (int i = 0; i < 10; i++) {
            System.out.println(slidingBloomFilter.contains(String.valueOf(i)));
        }

        long t1=System.currentTimeMillis();
        slidingBloomFilter.copy();
        System.out.println(System.currentTimeMillis()-t1);
        if (true)return;


        SlidingBloomFilterSnapshot snapshot = new SlidingBloomFilterSnapshot(slidingBloomFilter);
        System.out.println(JSON.toJSONString(snapshot, true));


        SlidingBloomFilter filter= snapshot.restore();
        System.out.println(  filter.contains("1"));
        System.out.println(  slidingBloomFilter.contains("1"));

    }
}
