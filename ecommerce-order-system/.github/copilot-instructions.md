# Copilot Instructions - Ecommerce Order System

## Project Overview

This is a **Spring Boot 3.2.3 + Kafka** multi-module Maven project demonstrating a distributed event-driven order processing workflow. It contains **5 modules** with microservices running on ports **8081-8084**.

### Modules
- **kafka-commons**: Shared events and enums across services
- **order-service** (8081): REST API for creating orders; produces `order-created` events
- **inventory-service** (8082): Consumes `order-created`; simulates reservation (10% failure rate)
- **payment-service** (8083): Consumes `order-created`; processes payments (15% failure rate); produces `payment-processed` events
- **notification-service** (8084): Consumes order and payment events; logs notifications

### Tech Stack
- **Java 17**, **Spring Boot 3.2.3**, **Spring Kafka**
- **H2 In-memory Database** (per service)
- **Maven** (multi-module build)
- **Docker/Docker Compose** for Kafka infrastructure (Zookeeper, Kafka broker, AKHQ UI on port 8080)
- **Springdoc-OpenAPI** for API documentation (Swagger UI, order-service)

---

## Build & Execution Commands

### Setup & Infrastructure
```bash
# Start Kafka infrastructure (Zookeeper, Kafka, AKHQ UI)
docker-compose up -d

# Build entire project
mvn clean install

# Build with tests
mvn clean install -DskipTests=false
```

### Run Services (choose one method)
```bash
# Option 1: Maven per module
mvn -pl order-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl payment-service spring-boot:run
mvn -pl notification-service spring-boot:run

# Option 2: IDE run configurations (recommended for debugging)
```

### Stop Infrastructure
```bash
docker-compose down
```

---

## Project Conventions

### Package Structure
All packages follow: `com.example.<service-name>.<layer>`
- `config/` - Kafka producer/consumer configs, Spring beans
- `controller/` - REST endpoints (order-service only)
- `service/` - Business logic
- `entity/` - JPA entities
- `dto/` - Request/response DTOs
- `repository/` - Spring Data JPA repositories
- `producer/` - Kafka event publishers
- `consumer/` - @KafkaListener methods
- `exception/` - Custom exceptions

### Kafka Configuration Patterns

#### Producer (order-service)
- Configured in `KafkaProducerConfig`
- Key serializer: `StringSerializer`
- Value serializer: `JsonSerializer`
- Acks: `"all"`, Retries: `3`
- Trusted packages: `"com.example.common.events"`

#### Consumer (inventory-service, payment-service)
- Configured in `KafkaConsumerConfig`
- Group ID: `<service>-group` (e.g., `inventory-group`)
- Manual ACK mode (`ContainerProperties.AckMode.MANUAL`)
- **Error handling with Dead Letter Topics (DLT)**:
  - `DeadLetterPublishingRecoverer` routes failed messages to `.DLT` topics
  - `ExponentialBackOff`: initial 1000ms, multiplier 2.0, max 10s, total 30s max
  - Failed messages sent to `{topic-name}.DLT` after exhausting retries

### Configuration Files
Each service has `src/main/resources/application.yml`:
```yaml
server:
  port: <service-port>
spring:
  application:
    name: <service-name>
  kafka:
    bootstrap-servers: localhost:9092
    properties:
      spring.json.trusted.packages: "com.example.common.events"
```

### Kafka Topics & Event Classes
Topics created by `docker-compose kafka-setup`:
- `order-created` → `OrderEvent` class in kafka-commons
- `payment-processed` → `PaymentEvent` class in kafka-commons
- `order-status-update` (defined, not actively used)
- `.DLT` variants for dead letters

### Resilience Features
1. **Dead Letter Topics**: Messages fail after retries → sent to `.DLT` topics for manual inspection
2. **Exponential Backoff**: Retries with increasing delays (1s → 10s max over 30s window)
3. **Idempotency**: payment-service deduplicates by `orderId` to prevent double-charging
4. **MDC Logging**: `orderId` included in all logs for distributed tracing (when implemented)

### Testing Patterns
- **Unit Tests**: Use `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks`
- **Integration Tests**: Use `@SpringBootTest` + `@EmbeddedKafka` for Kafka testing
- **Example test files**:
  - `OrderServiceTest` - service layer with mocks
  - `OrderKafkaIntegrationTest` - end-to-end with embedded Kafka

### Logging
- Use `lombok.extern.slf4j.Slf4j` annotation for logger injection
- Log order creation, event consumption, payments, and errors
- Track request flow with correlation IDs when implementing tracing

---

## API Documentation & Testing

### Swagger UI Access
- **Order Service API Docs**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs

### Available Endpoints

#### Order Service (Port 8081)
- `POST /orders` - Create a new order (produces `order-created` event)
  - Request body: `CreateOrderRequest` with customerId, customerEmail, items[]
  - Response: `OrderResponse` with generated orderId, status, and total amount

### Adding API Documentation
- Annotate controller methods with `@Operation`, `@ApiResponse`, `@Parameter` from `io.swagger.v3.oas.annotations`
- Configure API metadata in `OpenApiConfig.java` (title, description, contact, servers)
- Springdoc automatically generates documentation from annotations and validates them

---

## Common Development Tasks

### Adding a New Consumer Listener
1. Create handler method in service class
2. Annotate with `@KafkaListener(topics = "topic-name", groupId = "group-name")`
3. Configure error handling in `KafkaConsumerConfig`
4. Add integration test using `@EmbeddedKafka`

### Adding a New Event
1. Define event class in `kafka-commons` (e.g., `PaymentEvent`)
2. Add to `OrderEvent` or create new topic
3. Update bootstrap in `docker-compose.yml`
4. Update `spring.json.trusted.packages` in consumers

### Testing Order Flow
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "user@example.com",
    "items": [{"productId": "PROD-101", "productName": "Laptop", "quantity": 1, "unitPrice": 1200.00}]
  }'
```
Then observe logs in all services for event propagation.

### Debugging DLT Issues
1. View topics in AKHQ UI: http://localhost:8080
2. Check `.DLT` topics for failed messages
3. Inspect `ExponentialBackOff` config if retries too fast/slow
4. Verify consumer group offsets and lag

---

## Troubleshooting

### Kafka Connection Errors
- Verify `docker-compose up -d` is running: `docker ps | grep kafka`
- Check `bootstrap-servers: localhost:9092` in application.yml
- Ensure ports 9092, 2181, 8080 are not in use

### JSON Deserialization Failures
- Verify `spring.json.trusted.packages: "com.example.common.events"`
- Ensure event class version matches producer/consumer
- Check `JsonDeserializer` configuration includes `addTrustedPackages("*")` in tests

### Manual ACK Not Working
- Verify `ContainerProperties.AckMode.MANUAL` in `KafkaConsumerConfig`
- Ensure `Acknowledgment.acknowledge()` called after processing
- Check error handler behavior with `DeadLetterPublishingRecoverer`

### Test Failures
- Use `@EmbeddedKafka` for integration tests (not mock Kafka)
- Verify `@ActiveProfiles("test")` in test classes
- Ensure `ContainerTestUtils.waitForAssignment()` called before assertions

---

## Anti-Patterns to Avoid

1. **Auto-commit in manual mode** - Will lose DLT functionality
2. **Trusting all packages** (`"*"`) in production - Use explicit package lists
3. **Not handling idempotency** - Payment processing must check for duplicate `orderId`
4. **Ignoring DLT topics** - Failed messages silently lost if not monitored
5. **Hardcoding `localhost:9092`** - Use `@Value` injection from properties
6. **Synchronous Kafka waits** - Fire-and-forget pattern; don't block request threads
7. **Logging sensitive data** - Never log email, payment info; use correlation IDs instead

---

## When to Ask for Clarification

Before making changes, clarify:
- Whether to add breaking changes to event schemas (requires versioning)
- If new resilience patterns should be added (retry logic, circuit breaker)
- Who owns manual DLT reconciliation and monitoring
- If distributed tracing (OpenTelemetry/Jaeger) should be implemented
- Performance requirements (partitions, batch size, consumer threads)
