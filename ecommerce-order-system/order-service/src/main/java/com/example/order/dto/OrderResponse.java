package com.example.order.dto;

import com.example.common.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
}
