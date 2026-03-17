package com.example.order.service;

import com.example.common.enums.OrderStatus;
import com.example.common.events.OrderEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.producer.OrderKafkaProducer;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderKafkaProducer orderKafkaProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldSaveOrderAndPublishEvent() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("cust-1");
        request.setCustomerEmail("cust@example.com");
        
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("prod-1");
        item.setProductName("Product 1");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("100.00"));
        request.setItems(Collections.singletonList(item));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderKafkaProducer, times(1)).sendOrderEvent(any(OrderEvent.class));
    }
}
