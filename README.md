# Order Service with Transactional Outbox and Kafka Integration

This Spring Boot project demonstrates a **transactional outbox pattern** to ensure reliable message delivery when placing an order. Events are stored in the database before being published to Kafka.

---

## ✅ Features

- Order placement with product stock locking
- Transactional outbox event storage
- Kafka event publishing via retryable scheduler
- Retry logic for downstream notification delivery
- Dead-letter handling for notification failures
- WebClient-based async notification to downstream service

---

## 📦 Technologies

| **Technology**       | **Description**               |
|-----------------------|-------------------------------|
| **Java 17+**          | Programming language         |
| **Spring Boot**       | Backend framework            |
| **JPA**               | ORM for database operations  |
| **Kafka**             | Event streaming platform     |
| **KafkaTemplate**     | Kafka integration in Spring  |
| **MySQL/PostgreSQL**  | Relational database          |
| **WebClient**         | Async HTTP client            |

---

## 📘 Flow Overview

1. **OrderController** receives a new order.
2. **OrderService**:
   - Locks product stock with `PESSIMISTIC_WRITE`.
   - Deducts stock and saves the order.
   - Serializes and stores `OutboxEvent` in the DB (same transaction).
3. **OutboxKafkaPublisher**:
   - Scheduled job picks unprocessed `ORDER_CREATED` events.
   - Sends to Kafka topic `order-notifications`.
   - Marks event as processed on success.
4. **NotificationConsumer**:
   - Consumes from Kafka.
   - Asynchronously calls `http://localhost:8080/notify`.
   - On success, logs `NOTIFY_SUCCESS` event.
   - On failure (3 retries), logs `NOTIFY_FAILED` event.
5. **OutboxRetryScheduler**:
   - Scheduled job re-attempts failed notifications.

---

## 🌐 API Endpoints

### 1. Place an Order
**POST** `/order`

```json
{
  "userId": 1,
  "productIds": [1, 2, 3]
}

2. Notify Service

POST /notify

JSON
{
  "request": ""
}
✅ Non-Functional Requirements

1. Reliability

Outbox pattern ensures events are never lost, even if Kafka or /notify is down.
Kafka acts as a durable buffer, decoupling placeOrder from notification delivery.
/notify is retried asynchronously with capped retries and scheduled fallback via OutboxRetryScheduler.
2. Performance

Async notification avoids blocking the main request.
Kafka handles massive throughput efficiently.
Retries are isolated from the main user flow, keeping latency low.
Frequent full table scans on the outbox_event table can degrade performance as it grows, so create an index on processed, eventType, and optionally createdAt. Fetch events in batches.
3. Low Latency

User request (/order) completes fast without waiting for /notify.
Kafka-based fanout is fast and scalable.
✅ Transactional Boundaries in Service A

Transactional boundaries should encapsulate all DB operations in Service A. The REST call to Service B should be outside this boundary to avoid long-running transactions. Use a pattern like:

Begin DB transaction.
Perform local DB operations (e.g., persist request metadata).
Commit transaction.
Call Service B.
Based on Service B’s response:

Update status asynchronously.
Use a transactional outbox to ensure data consistency as Service B’s call needs to trigger further state changes.
✅ Threading Model and Implications

Service A performs blocking I/O (e.g., REST to B), which may hold the thread. To optimize:
Use WebClient (Reactor-based) for non-blocking I/O.
✅ Failure Scenarios

1. Network Issues Between A and B

a. Service B Unreachable:

Retry with exponential backoff.
Use circuit breaker to avoid cascading failures.
b. Timeouts/Lost Connections:

Set timeouts for WebClient.
Log failure, update state to PENDING, and process later using a scheduled retry mechanism.
2. Service A Crash

a. Possible Inconsistencies:

Crash before DB commit → no inconsistency.
Crash after DB commit but before calling B → partial update.
Crash after calling B but before response processed → uncertain final state.
b. Reconciliation Strategy:

Store intent or operation state (e.g., OUTBOX, PENDING).
On restart, a reconciliation job checks incomplete records and retries external calls or compensates.
Use idempotent APIs between A and B to support safe reprocessing.

✅ Sketch Diagram

Client --> Service A (Controller)
                   |
                   v
              Transactional Service
              | DB Commit |
                   |
            [Call Service B]
                   |
               Update Status
