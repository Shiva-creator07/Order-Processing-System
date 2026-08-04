# Order Processing System

An event-driven order processing system built with **Spring Boot**, **Apache Kafka**, **PostgreSQL**, and **Docker Compose**, implementing a **choreographed saga pattern** across four independent microservices.

Rather than relying on a central orchestrator, each service reacts to events published by the others, maintains its own database, and publishes its own result events — including compensating actions when something fails downstream.

---

## Architecture

```
                        ┌─────────────────┐
                        │  order-service   │  (REST API, port 8081)
                        │  order_db        │
                        └────────┬─────────┘
                                 │ publishes: order.created
                                 ▼
                        ┌─────────────────┐
                        │ inventory-service│  (port 8082)
                        │  inventory_db    │
                        └────────┬─────────┘
                                 │ publishes: inventory.events
                                 │ (RESERVED or FAILED)
                    ┌────────────┴────────────┐
                    ▼                         ▼
        (RESERVED — continue)      (FAILED — compensate)
                    │                         │
                    ▼                         ▼
        ┌─────────────────┐         ┌──────────────────┐
        │ payment-service  │         │  order-service    │
        │  payment_db      │         │  marks CANCELLED  │
        └────────┬─────────┘         └──────────────────┘
                  │ publishes: payment.events
                  │ (COMPLETED or FAILED)
                  ▼
        ┌─────────────────┐
        │  order-service    │
        │  marks CONFIRMED  │
        │  (or CANCELLED)   │
        └─────────────────┘

        All services also consume relevant events into:
        ┌──────────────────────┐
        │ notification-service │  (port 8084)
        │ logs/sends updates   │
        └──────────────────────┘
```

Kafka and Zookeeper coordinate messaging between all services. PostgreSQL provides a separate database per service (database-per-service pattern), and Docker Compose orchestrates startup order using healthchecks so dependent services don't start before Kafka and Postgres are actually ready.

---

## Tech Stack

- **Java 17 / Spring Boot 3.5**
- **Spring Cloud Stream** (Kafka binder, functional `Supplier`/`Function`/`Consumer` bindings — not `@KafkaListener`)
- **Apache Kafka + Zookeeper** (Confluent images, `cp-kafka:7.6.0`)
- **PostgreSQL 16** (one database per service)
- **Docker Compose** for local orchestration
- **Kafka UI** (provectuslabs) for inspecting topics/messages during development

---

## Services & Ports

| Service               | Port | Database       | Role                                              |
|------------------------|------|-----------------|----------------------------------------------------|
| `order-service`         | 8081 | `order_db`       | REST API for placing orders; saga coordinator via events |
| `inventory-service`     | 8082 | `inventory_db`   | Reserves/releases stock                            |
| `payment-service`       | 8083 | `payment_db`     | Processes payment for reserved orders               |
| `notification-service`  | 8084 | —                | Consumes all events, sends/logs status updates      |
| `kafka-ui`               | 8090 | —                | Web UI for inspecting Kafka topics                  |
| `postgres`               | 5432 | —                | Shared Postgres instance, 3 logical databases       |
| `kafka`                  | 9092 | —                | Message broker                                       |
| `zookeeper`               | 2181 | —                | Kafka coordination                                    |

Kafka topics: `order.created`, `inventory.events`, `payment.events`

---

## Running Locally

```bash
docker-compose up -d --build
```

First build takes several minutes (Maven dependency resolution per service). Subsequent builds are much faster due to Docker layer caching.

Check everything is healthy:

```bash
docker-compose ps
```

`postgres` and `kafka` should show `healthy` before the four application services report `Started`.

---

## Example: Placing an Order

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-002",
    "productId": "PROD-001",
    "quantity": 2,
    "totalAmount": 49.99
  }'
```

Check the resulting order status:

```bash
curl http://localhost:8081/api/orders/{orderId}
```

### Happy path (verified)

```
order-service:        PENDING
inventory-service:    RESERVED   (stock reserved successfully)
payment-service:      COMPLETED  (transaction ID issued)
order-service:        CONFIRMED
```

### Compensation path (verified)

If the requested product doesn't exist (or has insufficient stock), the saga fails gracefully instead of proceeding to payment:

```
order-service:        PENDING
inventory-service:    FAILED     (reason: Product not found)
order-service:        CANCELLED  (statusReason: Product not found)
payment-service:      never triggered
```

This is the core of the saga pattern — each service reacts only to what actually happened upstream, and failures propagate backward as compensating status updates rather than leaving orders in an inconsistent state.

---

## Known Limitations / Next Steps

- No automated test suite yet (unit/integration tests) — verification so far has been manual, via API calls and direct Postgres/Kafka inspection.
- No payment failure → inventory release compensation tested yet (only inventory failure is currently exercised).
- No retry/dead-letter-queue handling for Kafka consumer failures.
- No authentication/authorization on the REST APIs.
- No centralized logging/tracing across services (e.g. distributed tracing via Sleuth/Zipkin) — currently correlated manually via `orderId` in logs.

---

## Author

Shivansh Mishra