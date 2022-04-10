package com.gitbitex.matchingengine;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SlidingBloomFilterSnapshot {
    private int expectedInsertions;
    private int idx;
    private List<String> bloomFilterEncodedDataList = new ArrayList<>();

    public SlidingBloomFilterSnapshot(SlidingBloomFilter filter)  {
        this.expectedInsertions = filter.getExpectedInsertions();
        this.idx = filter.getIdx();
        for (BloomFilter<String> bloomFilter : filter.getFilters()) {
            if (bloomFilter != null) {
                this.bloomFilterEncodedDataList.add(encodeBloomFilter(bloomFilter));
            }
        }
    }

    public SlidingBloomFilter restore() {
        List<BloomFilter<String>> filters = this.bloomFilterEncodedDataList.stream()
                .map(x -> decodeBloomFilter(x))
                .collect(Collectors.toList());
        return new SlidingBloomFilter(this.expectedInsertions, this.idx, filters);
    }

    @SneakyThrows
    private static String encodeBloomFilter(BloomFilter<String> filter)  {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            filter.writeTo(stream);
            byte[] bytes = stream.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    @SneakyThrows
    private static BloomFilter<String> decodeBloomFilter(String base64String) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            return BloomFilter.readFrom(stream, Funnels.stringFunnel(StandardCharsets.UTF_8));
        }
    }
}
