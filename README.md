# Event-Driven Order Processing System

A choreographed-saga order processing backend built with Spring Boot, Spring Cloud Stream, and
Kafka. Four independently deployable services communicate only through events — no service calls
another service's REST API directly.

```
                         ┌──────────────────┐
        POST /orders     │                  │
   ─────────────────────►│  order-service   │◄────────────────┐
                          │  (order_db)      │                 │
                          └────────┬─────────┘                 │
                                   │ publishes                 │ consumes
                                   ▼                            │
                            topic: order.created                │
                                   │                            │
                    ┌──────────────┼───────────────┐            │
                    ▼              ▼               ▼            │
           ┌─────────────────┐          ┌────────────────────┐ │
           │ inventory-service│          │notification-service│ │
           │ (inventory_db)   │          │   (stateless)       │ │
           └────────┬─────────┘          └────────────────────┘ │
                     │ publishes                                │
                     ▼                                          │
              topic: inventory.events ──────────────────────────┤
                     │                                          │
                     ▼                                          │
           ┌──────────────────┐        ┌─────────────────────┐  │
           │  payment-service │        │ notification-service│  │
           │  (payment_db)    │        │                      │  │
           └────────┬─────────┘        └─────────────────────┘  │
                     │ publishes                                │
                     ▼                                          │
              topic: payment.events ─────────────────────────────┘
                     │
                     ▼
           ┌─────────────────────┐
           │ notification-service│
           └─────────────────────┘
```

## The saga, step by step

1. **Client calls `POST /api/orders`** on order-service. An `Order` row is saved with status
   `PENDING` and an `OrderCreatedEvent` is published to `order.created`.
2. **inventory-service** consumes `order.created`, checks stock in its own Postgres database,
   reserves (or fails to reserve) the requested quantity, and publishes an `InventoryResultEvent`
   (`RESERVED` / `FAILED`) to `inventory.events`.
3. **payment-service** consumes `inventory.events`. It only attempts a (simulated) charge if
   inventory was `RESERVED` — no point charging a customer for stock that doesn't exist. It
   publishes a `PaymentResultEvent` (`COMPLETED` / `FAILED`) to `payment.events`.
4. **order-service** also consumes `inventory.events` and `payment.events` to update the order's
   status as the saga progresses: `PENDING → INVENTORY_RESERVED → CONFIRMED`, or
   `→ INVENTORY_FAILED/CANCELLED` / `→ PAYMENT_FAILED` on the unhappy paths.
5. **notification-service** independently fans in from all three topics and logs a simulated
   customer notification for each meaningful state change (order received, stock issue, payment
   result). It has no database — it's a pure event listener, which is the point: adding a new
   side-effect to the system required zero changes to any other service.

This is a **choreographed saga**, not orchestration — there's no central coordinator telling
services what to do. Each service reacts to events and decides its own next action. That's a
deliberate design choice worth being able to defend in an interview (vs. an orchestrator/saga
manager approach), see "Talking points" below.

## Stack

- Java 17, Spring Boot 3.3
- Spring Cloud Stream (functional programming model: `Function`/`Consumer` beans) with the Kafka
  binder — no manual `KafkaTemplate`/`@KafkaListener` wiring
- PostgreSQL — one logical database per service (`order_db`, `inventory_db`, `payment_db`),
  enforcing that services never share tables
- Docker Compose — Kafka + Zookeeper, Postgres, Kafka UI, and all four services
- Lombok, Bean Validation, Spring Boot Actuator

## Project layout

```
order-processing-system/
├── docker-compose.yml
├── init-db/                       # creates the 3 databases on Postgres startup
├── order-service/                 # :8081 - REST API + saga entry/exit point
├── inventory-service/             # :8082 - stock reservation
├── payment-service/                # :8083 - simulated payment
├── notification-service/          # :8084 - stateless notification fan-in
└── test-saga.sh                   # curl smoke test walking through the whole flow
```

## Running it

Requires Docker and Docker Compose. (Maven dependency resolution happens inside the build
containers, so you need internet access the first time you build.)

```bash
docker compose up --build
```

This starts, in order of dependency: Zookeeper → Kafka → Kafka UI (localhost:8090) → Postgres
(with the 3 databases pre-created) → the four Spring Boot services.

Give it 30–60 seconds on first boot for Kafka and Postgres to be ready before the services finish
connecting.

### Try it

```bash
chmod +x test-saga.sh
./test-saga.sh
```

Or manually:

```bash
# Happy path — PROD-001 has 50 units in stock
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-100","productId":"PROD-001","quantity":2,"totalAmount":49.98}'

# Check status a couple seconds later — should move to CONFIRMED (or PAYMENT_FAILED ~10% of the time,
# since payment failure is randomly simulated to make the demo realistic)
curl http://localhost:8081/api/orders/{id}
```

Watch it happen live:

```bash
docker compose logs -f order-service inventory-service payment-service notification-service
```

Or visually inspect topics/messages/consumer groups at **http://localhost:8090** (Kafka UI) —
great for a screen recording or screenshot for your portfolio.

### Endpoints

| Service | Endpoint | Purpose |
|---|---|---|
| order-service | `POST /api/orders` | Create an order (starts the saga) |
| order-service | `GET /api/orders/{id}` | Get one order + current status |
| order-service | `GET /api/orders` | List all orders |
| inventory-service | `GET /api/inventory` | See current stock levels |
| payment-service | `GET /api/payments` | See all simulated transactions |
| payment-service | `GET /api/payments/order/{orderId}` | Transactions for one order |

## Talking points for interviews

- **Why choreography over orchestration?** No single point of failure/bottleneck, services stay
  loosely coupled, easy to add new consumers (notification-service) without touching upstream
  services. Trade-off: harder to see the "whole flow" in one place, and there's no built-in
  compensation/rollback coordinator — you'd reach for something like a Saga orchestrator
  (Camunda, or a dedicated orchestration service) if the flow got much more complex, or if you
  needed guaranteed compensating transactions across many steps.
- **Database-per-service** — each service owns its schema; nothing reaches into another
  service's tables. This is what actually makes it a microservices architecture rather than a
  monolith with some queues bolted on.
- **At-least-once delivery** — Kafka consumer groups here don't currently implement idempotency
  keys, so a redelivered message could double-reserve stock or double-charge. A natural follow-up
  improvement (and a good thing to mention proactively) is adding an idempotency/dedup table
  keyed on `orderId` + event type before writing.
- **Event enrichment vs. lookups** — `InventoryResultEvent` carries `customerId` and
  `totalAmount` forward so payment-service never needs to call back to order-service. This avoids
  synchronous service-to-service coupling but means event schemas grow; worth discussing schema
  evolution / a schema registry (Avro + Confluent Schema Registry) as the next step.
- **Failure simulation** — payment-service randomly fails ~10% of transactions
  (`payment.simulated-failure-rate`) so you can demonstrate both the happy path and the
  `PAYMENT_FAILED` path without extra tooling.

## Possible extensions (good "what would you add next" answers)

- Idempotent consumers (dedup table or Kafka transactional producer + `read_committed`)
- Dead-letter topics for poison messages
- An outbox pattern in order-service instead of dual-writing to Postgres + Kafka in one method
- Schema registry (Avro/Protobuf) instead of raw JSON
- A saga orchestrator variant for comparison, if you want to show both patterns
- Testcontainers-based integration tests spinning up real Kafka + Postgres per service
