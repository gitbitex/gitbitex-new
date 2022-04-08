package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDto {
    private String id;
    private String price;
    private String size;
    private String funds;
    private String productId;
    private String side;
    private String type;
    private String createdAt;
    private String fillFees;
    private String filledSize;
    private String executedValue;
    private String status;
}
