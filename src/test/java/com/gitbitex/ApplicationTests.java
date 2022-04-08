package com.gitbitex;

import com.alibaba.fastjson.JSON;

import com.gitbitex.order.entity.Order;
import com.gitbitex.product.entity.Product;
import com.gitbitex.order.OrderManager;
import com.gitbitex.order.repository.OrderRepository;
import com.gitbitex.product.repository.ProductRepository;
import com.gitbitex.openapi.controller.OrderController;
import com.gitbitex.openapi.model.OrderDto;
import com.gitbitex.openapi.model.PlaceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;

@SpringBootTest
class ApplicationTests {
    String productId = "BTC-USDT";
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderController orderController;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        //AppConfig.keyPrefix= UUID.randomUUID().toString();

        Product product = new Product();
        product.setProductId(productId);
        product.setBaseCurrency("BTC");
        product.setQuoteCurrency("USDT");
        product.setBaseScale(4);
        product.setQuoteScale(2);
        //product.setBaseMaxSize(new BI);
        productRepository.save(product);

        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    void contextLoads() throws Exception {
        {
            String orderId = placeOrder(productId, "limit", "buy", "1", "1");
            Thread.sleep(20000);
            Order order = orderRepository.findByOrderId(orderId);
            Assert.isTrue(order.getStatus() == Order.OrderStatus.OPEN);
        }



    }

    private String placeOrder(String productId, String type, String side, String size, String price)
        throws Exception {
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setProductId("BTC-USDT");
        placeOrderRequest.setType("limit");
        placeOrderRequest.setSide("buy");
        placeOrderRequest.setSize("1");
        placeOrderRequest.setPrice("1");

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
            .post("/orders")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(JSON.toJSONString(placeOrderRequest))

        )
            .andDo(new ResultHandler() {
                @Override
                public void handle(MvcResult result) throws Exception {
                    if (result.getResolvedException() != null) {
                        result.getResolvedException().printStackTrace();
                    }
                    System.err.println(result.getResponse().getContentAsString());
                }
            })
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        OrderDto orderDto = JSON.parseObject(mvcResult.getResponse().getContentAsString(), OrderDto.class);
        return orderDto.getId();
    }

}
