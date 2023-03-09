package com.gitbitex.matchingengine.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderLog extends Log {
    private String productId;
}
