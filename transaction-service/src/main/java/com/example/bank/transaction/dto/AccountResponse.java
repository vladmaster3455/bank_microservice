package com.example.bank.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        String currency,
        String status
) {
}
