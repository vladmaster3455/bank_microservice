package com.example.bank.transaction.dto;

import java.math.BigDecimal;

public record BalanceOperationRequest(BigDecimal amount, String reference) {
}
