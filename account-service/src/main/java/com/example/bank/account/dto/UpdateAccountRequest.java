package com.example.bank.account.dto;

import com.example.bank.account.domain.AccountStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
        @NotBlank String ownerName,
        AccountStatus status
) {
}
