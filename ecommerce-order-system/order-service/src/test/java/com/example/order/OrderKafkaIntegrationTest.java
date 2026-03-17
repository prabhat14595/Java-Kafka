package com.example.order;

import com.example.common.events.OrderEvent;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"order-created"}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
class OrderKafkaIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateOrderIntegration() throws Exception {
        // Set up Kafka consumer for testing
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        JsonDeserializer<OrderEvent> deserializer = new JsonDeserializer<>(OrderEvent.class);
        deserializer.addTrustedPackages("*");
        
        DefaultKafkaConsumerFactory<String, OrderEvent> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new org.apache.kafka.common.serialization.StringDeserializer(), deserializer);
        
        ContainerProperties containerProperties = new ContainerProperties("order-created");
        KafkaMessageListenerContainer<String, OrderEvent> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
        
        BlockingQueue<ConsumerRecord<String, OrderEvent>> records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, OrderEvent>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // Create Request
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("user-123");
        request.setCustomerEmail("user@example.com");
        
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("p-1");
        item.setProductName("Phone");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("999.99"));
        request.setItems(Collections.singletonList(item));

        // Hit API
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity("/orders", request, OrderResponse.class);

        // Assert API Response
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        // Assert Kafka Message
        ConsumerRecord<String, OrderEvent> received = records.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(request.getCustomerId(), received.value().getCustomerId());
        assertEquals(new BigDecimal("999.99"), received.value().getTotalAmount());

        container.stop();
    }
}
