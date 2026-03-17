package com.example.inventory.consumer;

import com.example.common.events.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DltConsumer {

    @KafkaListener(topics = "order-created.DLT", groupId = "inventory-dlt-group")
    public void consumeDlt(OrderEvent event, 
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(value = "x-exception-message", required = false) byte[] exceptionMessage) {
        String errorMessage = exceptionMessage != null ? new String(exceptionMessage) : "Unknown error";
        log.error("Message received in DLT: {} from topic: {} with error: {}", event, topic, errorMessage);
    }
}
