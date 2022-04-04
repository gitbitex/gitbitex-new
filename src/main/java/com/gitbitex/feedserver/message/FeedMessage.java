package com.gitbitex.feedserver.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedMessage {
    private String type;
    private String productId;
    private String userId;
}