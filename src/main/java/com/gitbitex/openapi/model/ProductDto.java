package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {
    private String id;
    private String baseCurrency;
    private String quoteCurrency;
    private String baseMinSize;
    private String baseMaxSize;
    private String quoteIncrement;
    private int baseScale;
    private int quoteScale;
}
