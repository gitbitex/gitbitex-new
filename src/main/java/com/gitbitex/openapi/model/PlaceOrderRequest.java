package com.gitbitex.openapi.model;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceOrderRequest {
    private String clientOid;

    @NotBlank
    private String productId;

    @NotBlank
    private String size;

    private String funds;

    private String price;

    @NotBlank
    private String side;

    @NotBlank
    private String type;
    /**
     * [optional] GTC, GTT, IOC, or FOK (default is GTC)
     */
    private String timeInForce;
}
