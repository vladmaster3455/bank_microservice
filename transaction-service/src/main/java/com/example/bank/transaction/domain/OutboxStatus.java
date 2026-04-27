package com.example.bank.transaction.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
