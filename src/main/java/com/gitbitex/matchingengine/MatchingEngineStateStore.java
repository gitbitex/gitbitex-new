package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
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
    private OptimisticTransactionDB db;
    private List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

    public MatchingEngineStateStore() {
        initDb();
    }

    @SneakyThrows
    public void initDb() {
        try (ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            ColumnFamilyDescriptor defaultCfd = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts);
            ColumnFamilyDescriptor orderCfd = new ColumnFamilyDescriptor("order".getBytes(), cfOpts);
            ColumnFamilyDescriptor accountCfd = new ColumnFamilyDescriptor("account".getBytes(), cfOpts);
            ColumnFamilyDescriptor productCfd = new ColumnFamilyDescriptor("product".getBytes(), cfOpts);
            ColumnFamilyDescriptor tradeIdCfd = new ColumnFamilyDescriptor("tradeId".getBytes(), cfOpts);
            ColumnFamilyDescriptor sequenceCfd = new ColumnFamilyDescriptor("sequence".getBytes(), cfOpts);
            ColumnFamilyDescriptor commandOffsetCfd = new ColumnFamilyDescriptor("commandOffset".getBytes(), cfOpts);
            List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(defaultCfd, orderCfd, accountCfd, productCfd,
                tradeIdCfd, sequenceCfd, commandOffsetCfd);
            try (DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
                db = OptimisticTransactionDB.open(options, "rocksdb", cfDescriptors, columnFamilyHandles);
            }
        }
    }

    @SneakyThrows
    public void write(Long commandOffset, Set<Account> accounts, Set<Order> orders, Set<Product> products,
        Map<String, Long> tradeIds, Map<String, Long> sequences) {
        try (WriteOptions writeOptions = new WriteOptions();
             Transaction transaction = db.beginTransaction(writeOptions)) {
            ColumnFamilyHandle orderCfh = columnFamilyHandles.get(1);
            ColumnFamilyHandle accountCfh = columnFamilyHandles.get(2);
            ColumnFamilyHandle productCfh = columnFamilyHandles.get(3);
            ColumnFamilyHandle tradeIdCfh = columnFamilyHandles.get(4);
            ColumnFamilyHandle sequenceCfh = columnFamilyHandles.get(5);
            ColumnFamilyHandle commandOffsetCfh = columnFamilyHandles.get(6);

            Long savedCommandOffset = getCommandOffset();
            if (savedCommandOffset != null && commandOffset <= savedCommandOffset) {
                logger.info("ignore data: {} {}", commandOffset, savedCommandOffset);
                //return;
            }

            transaction.put(commandOffsetCfh, "commandOffset".getBytes(), commandOffset.toString().getBytes());

            for (Account account : accounts) {
                transaction.put(accountCfh, (account.getUserId() + "-" + account.getCurrency()).getBytes(),
                    JSON.toJSONString(account).getBytes());
            }
            for (Order order : orders) {
                if (order.getStatus() == OrderStatus.OPEN) {
                    transaction.put(orderCfh, order.getOrderId().getBytes(), JSON.toJSONString(order).getBytes());
                } else {
                    transaction.delete(orderCfh, order.getOrderId().getBytes());
                }
            }
            if (products != null) {
                for (Product product : products) {
                    transaction.put(productCfh, product.getProductId().getBytes(), JSON.toJSONString(product).getBytes());
                }
            }
            for (Entry<String, Long> entry : tradeIds.entrySet()) {
                transaction.put(tradeIdCfh, entry.getKey().getBytes(), entry.getValue().toString().getBytes());
            }
            for (Entry<String, Long> entry : sequences.entrySet()) {
                transaction.put(sequenceCfh, entry.getKey().getBytes(), entry.getValue().toString().getBytes());
            }
            transaction.commit();
            logger.info("------------------------------saved:{}", commandOffset);
        }
    }

    @SneakyThrows
    public Long getCommandOffset() {
        ColumnFamilyHandle commandOffsetCfh = columnFamilyHandles.get(6);
        byte[] value = db.get(commandOffsetCfh, "commandOffset".getBytes());
        if (value == null) {
            return null;
        }
        return Long.parseLong(new String(value));
    }

    public void forEachOrder(Consumer<Order> consumer) {
        System.out.println("----------------------");
        ColumnFamilyHandle orderCfh = columnFamilyHandles.get(1);
        try (RocksIterator iterator = db.newIterator(orderCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(JSON.parseObject(iterator.value(), Order.class));
            }
        }
    }

    public void forEachAccount(Consumer<Account> consumer) {
        System.out.println("----------------------");
        ColumnFamilyHandle accountCfh = columnFamilyHandles.get(2);
        try (RocksIterator iterator = db.newIterator(accountCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(JSON.parseObject(iterator.value(), Account.class));
            }
        }
    }

    public void forEachTradeId(BiConsumer<String, Long> consumer) {
        System.out.println("----------------------");
        ColumnFamilyHandle tradeIdCfh = columnFamilyHandles.get(4);
        try (RocksIterator iterator = db.newIterator(tradeIdCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
    }

    public Map<String,Long> getTradeIds(){
        Map<String,Long> tradeIds=new HashMap<>();
        ColumnFamilyHandle tradeIdCfh = columnFamilyHandles.get(4);
        try (RocksIterator iterator = db.newIterator(tradeIdCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                tradeIds.put(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
        return tradeIds;
    }

    public Map<String,Long> getSequences(){
        Map<String,Long> tradeIds=new HashMap<>();
        ColumnFamilyHandle sequenceCfh = columnFamilyHandles.get(5);
        try (RocksIterator iterator = db.newIterator(sequenceCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                tradeIds.put(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
        return tradeIds;
    }

    public void forEachSequence(BiConsumer<String, Long> consumer) {
        System.out.println("----------------------");
        ColumnFamilyHandle sequenceCfh = columnFamilyHandles.get(5);
        try (RocksIterator iterator = db.newIterator(sequenceCfh)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                consumer.accept(new String(iterator.key()), Long.parseLong(new String(iterator.value())));
            }
        }
    }

}
