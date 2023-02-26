package com.gitbitex.matchingengine.log;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderLog extends Log {
    private String productId;
}
