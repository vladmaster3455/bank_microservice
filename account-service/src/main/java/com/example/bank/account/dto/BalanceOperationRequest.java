package com.example.bank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceOperationRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String reference
) {
}
