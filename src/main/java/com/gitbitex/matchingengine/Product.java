package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class Product {
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    public Product copy(){
        Product copy=new Product();
        BeanUtils.copyProperties(this,copy);
        return copy;
    }
}
