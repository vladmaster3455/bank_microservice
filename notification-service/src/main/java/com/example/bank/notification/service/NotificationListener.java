package com.example.bank.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "notification-service")
    public void onMessage(String payload,
                          @Header(name = "eventId", required = false) String eventId,
                          @Header(name = "eventType", required = false) String eventType) {
        log.info("Notification received | eventId={} | eventType={} | payload={}", eventId, eventType, payload);
    }
}
