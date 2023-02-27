package com.gitbitex.marketdata.manager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.alibaba.fastjson.JSON;

import com.gitbitex.marketdata.entity.Fill;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.marketdata.repository.FillRepository;
import com.gitbitex.marketdata.repository.OrderRepository;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.or;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderManager {
    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;

    @Transactional(rollbackFor = Exception.class)
    public void fillOrder(String orderId, long tradeId, BigDecimal size, BigDecimal price, BigDecimal funds) {
        // check if fill has been executed
        Fill fill = fillRepository.findByOrderIdAndTradeId(orderId, tradeId);
        if (fill != null) {
            return;
        }

        Order order = orderRepository.findByOrderId(orderId);
        order.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().add(size) : size);
        order.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().add(funds) : funds);

        if (order.getSide() == OrderSide.BUY) {
            if (order.getExecutedValue().compareTo(order.getFunds()) > 0) {
                throw new RuntimeException("bad order: " + JSON.toJSONString(order));
            }
        } else {
            if (order.getFilledSize().compareTo(order.getSize()) > 0) {
                throw new RuntimeException("bad order: " + JSON.toJSONString(order));
            }
        }

        save(order);

        fill = new Fill();
        fill.setFillId(UUID.randomUUID().toString());
        fill.setOrderId(orderId);
        fill.setTradeId(tradeId);
        fill.setSize(size);
        fill.setPrice(price);
        fill.setFunds(funds);
        fill.setSide(order.getSide());
        fill.setProductId(order.getProductId());
        fill.setUserId(order.getUserId());
        fillRepository.save(fill);
    }


    public void receiveOrder(Order order) {
        logger.info("[{}] Receive order", order.getOrderId());

        if (findByOrderId(order.getOrderId()) != null) {
            logger.warn("Order already exists ：{}", order.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.RECEIVED);
        save(order);
    }

    public void rejectOrder(Order order) {
        logger.info("[{}] Reject order", order.getOrderId());

        if (findByOrderId(order.getOrderId()) != null) {
            logger.warn("Order already exists ：{}", order.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        save(order);
    }

    public void openOrder(String orderId) {
        logger.info("[{}] Open order", orderId);

        Order order = findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.RECEIVED) {
            logger.warn("[{}] Cannot open an order in a non-RECEIVED status,current status: {}", order.getOrderId(),
                    order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.OPEN);
        save(order);
    }

    public void closeOrder(String orderId, OrderStatus orderStatus) {
        logger.info("[{}] Close order: reason={}", orderId, orderStatus);

        Order order = findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.RECEIVED && order.getStatus() != OrderStatus.OPEN) {
            logger.warn("[{}] Cannot close an order in a non-RECEIVED or non-OPEN status,current status: {}",
                    order.getOrderId(), order.getStatus());
            return;
        }

        order.setStatus(orderStatus);
        save(order);
    }

    public void save(Order order) {
        orderRepository.save(order);

        // send order update notify
        /*try {
            redissonClient.getTopic("order", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(order));
        } catch (Exception e) {
            logger.error("notify error: {}", e.getMessage(), e);
        }*/
    }

    public Order findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    private final MongoClient mongoClient;

    CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
    CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));

    public void saveAll(List<Order> orders) {

        MongoCollection<Order> orderMongoCollection = mongoClient.getDatabase("ex").withCodecRegistry(pojoCodecRegistry)
                .getCollection("orders", Order.class);
        Bson query = gt("num_mflix_comments", 50);
        Bson updates = Updates.combine(
                Updates.addToSet("genres", "Frequently Discussed"),
                Updates.currentTimestamp("lastUpdated"));


        List<WriteModel<Order> > writeModels=new ArrayList<>();
        for (Order order : orders) {
            Bson filter = Filters.eq("_id", order.getOrderId());


            WriteModel<Order> orderWriteModel = new ReplaceOneModel<Order>(filter, order, new ReplaceOptions().upsert(true));
            writeModels.add(orderWriteModel);

        }
        orderMongoCollection.bulkWrite(writeModels);
    }

}
