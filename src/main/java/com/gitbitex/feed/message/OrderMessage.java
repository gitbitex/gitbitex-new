package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessage {
    private String type = "order";
    private String productId;
    private String userId;
    private String sequence;
    private String id;
    private String price;
    private String size;
    private String funds;
    private String side;
    private String orderType;
    private String createdAt;
    private String fillFees;
    private String filledSize;
    private String executedValue;
    private String status;
    private boolean settled;
}
