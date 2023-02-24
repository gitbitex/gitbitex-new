package com.gitbitex.feed.message;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest extends Request {
    private List<String> productIds;
    private List<String> currencyIds;
    private List<String> channels;
}
