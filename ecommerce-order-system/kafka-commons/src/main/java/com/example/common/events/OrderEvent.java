package com.example.common.events;

import com.example.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private UUID orderId;
    private String customerId;
    private String customerEmail;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
}
