package com.example.order.service;

import com.example.common.enums.OrderStatus;
import com.example.common.events.OrderEvent;
import com.example.common.events.OrderItem;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.producer.OrderKafkaProducer;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderKafkaProducer orderKafkaProducer;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        MDC.put("orderId", orderId.toString());
        try {
            BigDecimal totalAmount = request.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Order order = Order.builder()
                    .id(orderId)
                    .customerId(request.getCustomerId())
                    .customerEmail(request.getCustomerEmail())
                    .totalAmount(totalAmount)
                    .status(OrderStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

            orderRepository.save(order);
            log.info("Order saved to database with PENDING status");

            OrderEvent event = OrderEvent.builder()
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .customerEmail(request.getCustomerEmail())
                    .totalAmount(totalAmount)
                    .status(OrderStatus.PENDING)
                    .createdAt(order.getCreatedAt())
                    .items(request.getItems().stream()
                            .map(i -> OrderItem.builder()
                                    .productId(i.getProductId())
                                    .productName(i.getProductName())
                                    .quantity(i.getQuantity())
                                    .unitPrice(i.getUnitPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            orderKafkaProducer.sendOrderEvent(event);
            log.info("Order event published to Kafka");

            return mapToResponse(order);
        } finally {
            MDC.clear();
        }
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
