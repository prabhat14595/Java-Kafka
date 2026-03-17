# E-commerce Order Processing System (Spring Boot + Kafka)

This is a multi-module Maven project demonstrating a distributed order processing workflow using Apache Kafka.

## Modules

1.  **kafka-commons**: Shared events and DTOs used across services.
2.  **order-service**: Entry point for orders (Port 8081). Produces `order-created` events.
3.  **inventory-service**: Consumes `order-created` events. Simulates item reservation (Port 8082).
4.  **payment-service**: Consumes `order-created` events. Processes payments and produces `payment-processed` events (Port 8083).
5.  **notification-service**: Consumes both order and payment events to send "notifications" (logged to console) (Port 8084).

## Tech Stack
- Java 17
- Spring Boot 3.2.x
- Spring Kafka
- H2 In-memory Database
- Docker & Docker Compose
- AKHQ (Kafka UI)

## Getting Started

### 1. Start Kafka Infrastructure
```bash
docker-compose up -d
```
Access AKHQ (Kafka UI) at [http://localhost:8080](http://localhost:8080)

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run Services
Run each service using Maven or your IDE:
- `mvn -pl order-service spring-boot:run`
- `mvn -pl inventory-service spring-boot:run`
- `mvn -pl payment-service spring-boot:run`
- `mvn -pl notification-service spring-boot:run`

## Testing the Workflow

### Create an Order
```bash
curl -X POST http://localhost:8081/orders \
-H "Content-Type: application/json" \
-d '{
  "customerId": "CUST-001",
  "customerEmail": "user@example.com",
  "items": [
    {
      "productId": "PROD-101",
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 1200.00
    }
  ]
}'
```

### Observe Logs
- **Order Service**: Logs order saving and event publication.
- **Inventory Service**: Logs item reservation. (10% chance of failure to test DLT).
- **Payment Service**: Logs payment success/failure. (15% chance of failure).
- **Notification Service**: Logs confirmation and payment status emails.

## Resilience Features
- **Exponential Backoff**: Configured in `inventory-service` and `payment-service` consumers.
- **Dead Letter Topic (DLT)**: messages that fail after retries are sent to `.DLT` topics.
- **Idempotency**: `payment-service` checks if an `orderId` has already been processed.
- **MDC Logging**: `orderId` is included in all logs for distributed tracing.
