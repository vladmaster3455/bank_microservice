package com.example.bank.transaction.service;

import com.example.bank.transaction.domain.OutboxEvent;
import com.example.bank.transaction.domain.OutboxStatus;
import com.example.bank.transaction.domain.Transfer;
import com.example.bank.transaction.dto.TransferCompletedEvent;
import com.example.bank.transaction.exception.BusinessException;
import com.example.bank.transaction.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createTransferCompletedEvent(Transfer transfer) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("TRANSFER");
        event.setAggregateId(transfer.getId());
        event.setEventType("TRANSFER_COMPLETED");
        event.setStatus(OutboxStatus.PENDING);
        event.setPayload(serialize(new TransferCompletedEvent(
                UUID.randomUUID(),
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getTargetAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getIdempotencyKey(),
                OffsetDateTime.now()
        )));
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> loadPendingEvents() {
        return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Outbox event not found: " + eventId));
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(OffsetDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Outbox event not found: " + eventId));
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= 10) {
            event.setStatus(OutboxStatus.FAILED);
        }
        outboxEventRepository.save(event);
    }

    private String serialize(TransferCompletedEvent payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Unable to serialize outbox payload: " + ex.getMessage());
        }
    }
}
