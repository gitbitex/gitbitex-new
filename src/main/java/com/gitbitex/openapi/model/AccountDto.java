package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDto {
    private String id;
    private String currency;
    private String currencyIcon;
    private String available;
    private String hold;
}

