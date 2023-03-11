package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeDto {
    private long sequence;
    private String time;
    private String price;
    private String size;
    private String side;
}
