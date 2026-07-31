package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Запрос на создание кошелька
 */
@Getter
@Setter
public class CreateWalletRequest {
    @NotBlank
    private String currency;
}
