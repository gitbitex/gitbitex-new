package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Запрос на перевод между кошельками
 */
@Getter
@Setter
public class TransferRequest {
    @NotBlank
    private String fromWalletId;
    
    @NotBlank
    private String toWalletId;
    
    @NotNull
    private BigDecimal amount;
}
