package com.example.bank.account.dto;

import com.example.bank.account.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
