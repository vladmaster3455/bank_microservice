package com.example.bank.transaction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        OffsetDateTime occurredAt
) {
}
