package com.example.bank.transaction.service;

import com.example.bank.transaction.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxPublisher(OutboxService outboxService,
                           KafkaTemplate<String, String> kafkaTemplate,
                           @Value("${app.kafka.notification-topic}") String topic) {
        this.outboxService = outboxService;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxService.loadPendingEvents();
        for (OutboxEvent event : events) {
            try {
                Message<String> message = MessageBuilder.withPayload(event.getPayload())
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
                        .setHeader("eventId", event.getId().toString())
                        .setHeader("eventType", event.getEventType())
                        .build();
                kafkaTemplate.send(message).get();
                outboxService.markPublished(event.getId());
                log.info("Outbox event {} published to topic {}", event.getId(), topic);
            } catch (Exception ex) {
                outboxService.markFailed(event.getId());
                log.error("Failed to publish outbox event {}", event.getId(), ex);
            }
        }
    }
}
