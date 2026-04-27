package com.example.bank.account.dto;

import com.example.bank.account.domain.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String accountNumber,
        @NotBlank String ownerName,
        @NotNull @DecimalMin(value = "0.00") BigDecimal initialBalance,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO-4217") String currency,
        AccountStatus status
) {
}
