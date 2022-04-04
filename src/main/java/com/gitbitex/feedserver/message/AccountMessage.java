package com.gitbitex.feedserver.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountMessage extends FeedMessage {
    private String currencyCode;
    private String available;
    private String hold;
}
