package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountMessage {
    private String type = "funds";
    private String productId;
    private String userId;
    private String currencyCode;
    private String available;
    private String hold;
}
