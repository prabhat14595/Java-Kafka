package com.example.common.events;

import com.example.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private Instant processedAt;
}
