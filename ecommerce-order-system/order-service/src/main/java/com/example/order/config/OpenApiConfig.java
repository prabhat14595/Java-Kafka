package com.example.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Swagger UI documentation.
 * Provides API metadata and descriptions for the Order Service.
 * 
 * Access Swagger UI at: http://localhost:8081/swagger-ui.html
 * API Docs at: http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development"),
                        new Server().url("http://localhost").description("Production")
                ))
                .info(new Info()
                        .title("E-commerce Order Service API")
                        .description("""
                                Order Service provides REST endpoints for creating and managing orders.
                                
                                **Features:**
                                - Create orders with order items
                                - Publish order-created events to Kafka
                                - Event-driven architecture integration
                                
                                **Event Flow:**
                                1. POST /orders → Creates order and publishes order-created event
                                2. order-created event consumed by inventory-service (reservation)
                                3. order-created event consumed by payment-service (payment processing)
                                4. payment-processed event published by payment-service
                                5. Both events consumed by notification-service (logging)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Service Team")
                                .email("order-service@example.com")
                                .url("http://www.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
