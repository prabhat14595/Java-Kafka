package com.example.payment.consumer;

import com.example.common.enums.PaymentStatus;
import com.example.common.events.OrderEvent;
import com.example.common.events.PaymentEvent;
import com.example.payment.entity.Payment;
import com.example.payment.producer.PaymentKafkaProducer;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer paymentKafkaProducer;
    private final Random random = new Random();

    @Transactional
    @KafkaListener(topics = "order-created", groupId = "payment-group")
    public void consume(OrderEvent event, Acknowledgment acknowledgment) {
        MDC.put("orderId", event.getOrderId().toString());
        try {
            log.info("Processing payment for order: {}", event.getOrderId());

            // Idempotency check
            if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
                log.info("Payment already processed for order: {}. Skipping.", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // Simulate payment processing (random 15% failure)
            boolean isSuccess = random.nextInt(100) >= 15;
            PaymentStatus status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .amount(event.getTotalAmount())
                    .status(status)
                    .processedAt(Instant.now())
                    .build();

            paymentRepository.save(payment);
            log.info("Payment record saved with status: {}", status);

            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(event.getOrderId())
                    .amount(event.getTotalAmount())
                    .status(status)
                    .processedAt(payment.getProcessedAt())
                    .build();

            paymentKafkaProducer.sendPaymentEvent(paymentEvent);
            log.info("Payment event published for order: {}", event.getOrderId());
            
            acknowledgment.acknowledge();
        } finally {
            MDC.clear();
        }
    }
}
