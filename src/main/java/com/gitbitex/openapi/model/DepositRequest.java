package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Запрос на пополнение кошелька (депозит)
 */
@Getter
@Setter
public class DepositRequest {
    @NotBlank
    private String walletId;
    
    @NotNull
    private BigDecimal amount;
}
