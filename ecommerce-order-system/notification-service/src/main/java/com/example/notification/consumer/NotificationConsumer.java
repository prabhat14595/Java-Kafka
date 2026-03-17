package com.example.notification.consumer;

import com.example.common.events.OrderEvent;
import com.example.common.events.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-created", groupId = "notification-group")
    public void consumeOrderCreated(String message) {
        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            MDC.put("orderId", event.getOrderId().toString());
            log.info("EMAIL to {}: Your order {} has been confirmed", 
                    event.getCustomerEmail(), event.getOrderId());
        } catch (Exception e) {
            log.error("Error parsing OrderEvent: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "payment-processed", groupId = "notification-group")
    public void consumePaymentProcessed(String message) {
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            MDC.put("orderId", event.getOrderId().toString());
            log.info("NOTIFICATION: Payment for order {} was {}", 
                    event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Error parsing PaymentEvent: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }
}
