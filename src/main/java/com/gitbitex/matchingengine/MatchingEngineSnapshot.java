package com.gitbitex.matchingengine;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;

import com.gitbitex.matchingengine.LogWriter.DirtyObjectList;
import com.gitbitex.matchingengine.log.OrderLog;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksIterator;
import org.rocksdb.Transaction;
import org.rocksdb.WriteOptions;

@Getter
@Setter
@Slf4j
public class MatchingEngineSnapshot implements AutoCloseable {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds;
    private List<Product> products;
    private Set<Account> accounts;
    private Set<Order> orders;
    private List<OrderBookSnapshot> orderBookSnapshots;
    private long time;
    private long beginCommandOffset;
    private long endCommandOffset;
    private Map<String, Long> tradeIds;
    private Map<String, Long> sequences;

    public MatchingEngineSnapshot(){

    }


    @SneakyThrows
    private void createSnapshot() {
        long beginCommandOffset;
        long endCommandOffset;
        Set<Account> accounts = new HashSet<>();
        Set<Order> orders = new HashSet<>();
        Map<String, Long> tradeIdByProductId = new HashMap<>();
        Map<String, Long> sequenceByProductId = new HashMap<>();

        try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            // list of column family descriptors, first entry must always be default column family
            ColumnFamilyDescriptor defaultCfd = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts);
            ColumnFamilyDescriptor orderCfd = new ColumnFamilyDescriptor("order".getBytes(), cfOpts);
            ColumnFamilyDescriptor accountCfd = new ColumnFamilyDescriptor("account".getBytes(), cfOpts);
            ColumnFamilyDescriptor confCfd = new ColumnFamilyDescriptor("account".getBytes(), cfOpts);
            final List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(defaultCfd, orderCfd);

            // a list which will hold the handles for the column families once the db is opened
            final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

            try (final DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
                 final OptimisticTransactionDB db = OptimisticTransactionDB.open(options, "rocksdb", cfDescriptors,
                     columnFamilyHandleList);
                 WriteOptions writeOptions = new WriteOptions();
                 Transaction transaction = db.beginTransaction(writeOptions);

            ) {
                ColumnFamilyHandle orderCfh = columnFamilyHandleList.get(1);

                try {

                    for (Order order : orders) {
                        //if (order.getStatus()== OrderStatus.OPEN){
                        db.put(orderCfh, order.getOrderId().getBytes(), JSON.toJSONString(order).getBytes());
                        //}else{
                        //  db.delete(orderCfh,order.getOrderId().getBytes());
                        //}
                    }

                    transaction.commit();

                    System.out.println("----------------------");
                    try (RocksIterator orderIterator = db.newIterator(orderCfh)) {
                        for (orderIterator.seekToFirst(); orderIterator.isValid(); orderIterator.next()) {
                            System.out.println(
                                new String(orderIterator.key()) + "=" + new String(orderIterator.value()));
                        }
                    }

                } finally {

                    // NOTE frees the column family handles before freeing the db
                    for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleList) {
                        columnFamilyHandle.close();
                    }
                } // frees the db and the db options
            }
        } // frees the column family options

        if (true) {return;}
        ;

        if (beginCommandOffset == endCommandOffset) {
            logger.info("nonthing changed: {} {}", beginCommandOffset, endCommandOffset);
        } else {
            logger.info("taking snapshot :{} {}", beginCommandOffset, endCommandOffset);
        }

        MatchingEngineSnapshot snapshot = new MatchingEngineSnapshot();
        snapshot.setAccounts(accounts);
        snapshot.setOrders(orders);
        snapshot.setBeginCommandOffset(beginCommandOffset);
        snapshot.setEndCommandOffset(endCommandOffset);
        snapshot.setTradeIds(tradeIdByProductId);
        snapshot.setSequences(sequenceByProductId);
        System.out.println(JSON.toJSONString(snapshot, true));
    }

    @Override
    public void close() throws Exception {

    }
}
