package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.util.GZipUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;

@Getter
@Setter
public class FullOrderBookSnapshot implements Serializable {
    private String productId;
    private long sequence;
    private long time;
    private String compressionType;
    private String base64Data;

    public FullOrderBookSnapshot() {
    }

    public FullOrderBookSnapshot(OrderBook orderBook) {
        byte[] bytes = GZipUtils.compress(SerializationUtils.serialize(orderBook));
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        this.time = System.currentTimeMillis();
        this.base64Data = Base64.getEncoder().encodeToString(bytes);
    }

    public OrderBook decodeOrderBook() {
        byte[] bytes = Base64.getDecoder().decode(this.base64Data);
        return (OrderBook) SerializationUtils.deserialize(GZipUtils.uncompress(bytes));
    }
}
