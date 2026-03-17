package com.example.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String customerId;
    
    @Email
    @NotBlank
    private String customerEmail;
    
    @NotEmpty
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String productId;
        @NotBlank
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
