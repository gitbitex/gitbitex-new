package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UnsubscribeRequest extends Request {
    private List<String> productIds;
    private List<String> currencyIds;
    private List<String> channels;
}
