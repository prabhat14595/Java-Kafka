package com.example.payment.producer;

import com.example.common.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private static final String TOPIC = "payment-processed";

    public void sendPaymentEvent(PaymentEvent event) {
        log.info("Sending payment event for order: {}", event.getOrderId());
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }
}
