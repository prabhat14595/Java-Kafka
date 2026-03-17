package com.example.inventory.consumer;

import com.example.common.events.OrderEvent;
import com.example.inventory.exception.InventoryException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class InventoryConsumer {

    private final Random random = new Random();

    @KafkaListener(topics = "order-created", groupId = "inventory-group")
    public void consume(OrderEvent event, Acknowledgment acknowledgment) {
        MDC.put("orderId", event.getOrderId().toString());
        try {
            log.info("Processing inventory for order: {}", event.getOrderId());
            
            event.getItems().forEach(item -> {
                log.info("Reserving product: {} (quantity: {})", item.getProductId(), item.getQuantity());
            });

            // Simulate inventory check (random 10% failure for testing DLT)
            if (random.nextInt(100) < 10) {
                log.error("Inventory check failed for order: {}", event.getOrderId());
                throw new InventoryException("Inventory unavailable for one or more items");
            }

            log.info("Inventory successfully reserved for order: {}", event.getOrderId());
            acknowledgment.acknowledge();
        } finally {
            MDC.clear();
        }
    }
}
