package com.gitbitex.matchingengine.message;

import com.gitbitex.matchingengine.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductMessage extends Message {
    private Product product;

    public ProductMessage() {
        this.setMessageType(MessageType.PRODUCT);
    }
}
