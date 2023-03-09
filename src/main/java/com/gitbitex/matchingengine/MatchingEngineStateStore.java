package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;

import com.gitbitex.enums.OrderStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.Transaction;
import org.rocksdb.WriteOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MatchingEngineStateStore {
    static {
        RocksDB.loadLibrary();
    }

    private final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
    private OptimisticTransactionDB db;

    public MatchingEngineStateStore() {
        initDb();
    }

    @SneakyThrows
    public void initDb() {
        try (ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            List<ColumnFamilyDescriptor> cfDescriptors = columnFamilyDescriptors(cfOpts);
            try (DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
                db = OptimisticTransactionDB.open(options, "rocksdb", cfDescriptors, columnFamilyHandles);
            }
        }
    }

    public void close() {
        for (ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles) {
            columnFamilyHandle.close();
        }
        db.close();
    }

    @SneakyThrows
    public void write(Long commandOffset, Set<Account> accounts, Set<Order> orders, Set<Product> products,
        Map<String, Long> tradeIds, Map<String, Long> sequences) {
        ColumnFamilyHandle orderCfh = getOrderColumnFamilyHandle();
        ColumnFamilyHandle accountCfh = getAccountColumnFamilyHandle();
        ColumnFamilyHandle productCfh = getProductColumnFamilyHandle();
        ColumnFamilyHandle tradeIdCfh = getTradeIdColumnFamilyHandle();
        ColumnFamilyHandle sequenceCfh = getSequenceColumnFamilyHandle();
        ColumnFamilyHandle commandOffsetCfh = getCommandOffsetColumnFamilyHandle();

        try (WriteOptions options = new WriteOptions(); Transaction transaction = db.beginTransaction(options)) {
            transaction.put(commandOffsetCfh, "commandOffset".getBytes(), commandOffset.toString().getBytes());
            for (Account account : accounts) {
                transaction.put(accountCfh, (account.getUserId() + "-" + account.getCurrency()).getBytes(),
                    JSON.toJSONBytes(account));
            }
            for (Order order : orders) {
                if (order.getStatus() == OrderStatus.OPEN) {
                    transaction.put(orderCfh, order.getOrderId().getBytes(), JSON.toJSONBytes(order));
                } else {
                    transaction.delete(orderCfh, order.getOrderId().getBytes());
                }
            }
            for (Product product : products) {
                transaction.put(productCfh, product.getProductId().getBytes(), JSON.toJSONBytes(product));
            }
            for (Entry<String, Long> entry : tradeIds.entrySet()) {
                transaction.put(tradeIdCfh, entry.getKey().getBytes(), entry.getValue().toString().getBytes());
            }
            for (Entry<String, Long> entry : sequences.entrySet()) {
                transaction.put(sequenceCfh, entry.getKey().getBytes(), entry.getValue().toString().getBytes());
            }

            transaction.commit();
        }
    }

    @SneakyThrows
    public Long getCommandOffset() {
        ColumnFamilyHandle commandOffsetCfh = getCommandOffsetColumnFamilyHandle();
        byte[] value = db.get(commandOffsetCfh, "commandOffset".getBytes());
        if (value == null) {
            return null;
        }
        return Long.parseLong(new String(value));
    }

    public void forEachOrder(Consumer<Order> consumer) {
        ColumnFamilyHandle orderCfh = getOrderColumnFamilyHandle();
        try (RocksIterator iterator = db.newIterator(orderCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(JSON.parseObject(iterator.value(), Order.class));
            }
        }
    }

    public void forEachAccount(Consumer<Account> consumer) {
        ColumnFamilyHandle accountCfh = getAccountColumnFamilyHandle();
        try (RocksIterator iterator = db.newIterator(accountCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(JSON.parseObject(iterator.value(), Account.class));
            }
        }
    }

    public void forEachProduct(Consumer<Product> consumer) {
        ColumnFamilyHandle cfh = getProductColumnFamilyHandle();
        try (RocksIterator iterator = db.newIterator(cfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(JSON.parseObject(iterator.value(), Product.class));
            }
        }
    }

    public Map<String, Long> getTradeIds() {
        Map<String, Long> tradeIds = new HashMap<>();
        ColumnFamilyHandle tradeIdCfh = getTradeIdColumnFamilyHandle();
        try (RocksIterator iterator = db.newIterator(tradeIdCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                tradeIds.put(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
        return tradeIds;
    }

    public Map<String, Long> getSequences() {
        Map<String, Long> sequences = new HashMap<>();
        ColumnFamilyHandle sequenceCfh = getSequenceColumnFamilyHandle();
        try (RocksIterator iterator = db.newIterator(sequenceCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                sequences.put(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
        return sequences;
    }

    private List<ColumnFamilyDescriptor> columnFamilyDescriptors(ColumnFamilyOptions cfOpts) {
        return Arrays.asList(
            new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
            new ColumnFamilyDescriptor("order".getBytes(), cfOpts),
            new ColumnFamilyDescriptor("account".getBytes(), cfOpts),
            new ColumnFamilyDescriptor("product".getBytes(), cfOpts),
            new ColumnFamilyDescriptor("tradeId".getBytes(), cfOpts),
            new ColumnFamilyDescriptor("sequence".getBytes(), cfOpts),
            new ColumnFamilyDescriptor("commandOffset".getBytes()));
    }

    private ColumnFamilyHandle getOrderColumnFamilyHandle() {
        return columnFamilyHandles.get(1);
    }

    private ColumnFamilyHandle getAccountColumnFamilyHandle() {
        return columnFamilyHandles.get(2);
    }

    private ColumnFamilyHandle getProductColumnFamilyHandle() {
        return columnFamilyHandles.get(3);
    }

    private ColumnFamilyHandle getTradeIdColumnFamilyHandle() {
        return columnFamilyHandles.get(4);
    }

    private ColumnFamilyHandle getSequenceColumnFamilyHandle() {
        return columnFamilyHandles.get(5);
    }

    private ColumnFamilyHandle getCommandOffsetColumnFamilyHandle() {
        return columnFamilyHandles.get(6);
    }

}
