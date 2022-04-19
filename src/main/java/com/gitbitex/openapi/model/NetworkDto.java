package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetworkDto {
    private String status;
    private String hash;
    private String amount;
    private String feeAmount;
    private String feeCurrency;
    private int confirmations;
    private String resourceUrl;
}
