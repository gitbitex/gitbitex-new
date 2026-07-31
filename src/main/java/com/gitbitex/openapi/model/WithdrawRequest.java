package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Запрос на вывод средств
 */
@Getter
@Setter
public class WithdrawRequest {
    @NotBlank
    private String walletId;
    
    @NotNull
    private BigDecimal amount;
    
    @NotBlank
    private String address;
}
