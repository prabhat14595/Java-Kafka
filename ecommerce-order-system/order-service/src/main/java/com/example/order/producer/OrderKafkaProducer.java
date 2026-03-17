package com.example.order.producer;

import com.example.common.events.OrderEvent;
import com.example.order.exception.OrderPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "order-created";

    public void sendOrderEvent(OrderEvent event) {
        String key = event.getOrderId().toString();
        CompletableFuture<SendResult<String, OrderEvent>> future = kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}] to partition=[{}]", 
                        event, result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            } else {
                log.error("Unable to send message=[{}] due to : {}", event, ex.getMessage());
                throw new OrderPublishException("Failed to publish OrderEvent", ex);
            }
        });
    }
}
