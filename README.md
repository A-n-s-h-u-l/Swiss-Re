# Order Service with Transactional Outbox and Kafka Integration

This Spring Boot project demonstrates a **transactional outbox pattern** to ensure reliable message delivery when placing an order. Events are stored in the database before being published to Kafka, ensuring **strong consistency** between DB state and event dispatch.

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

- Java 17+, Spring Boot, JPA
- Kafka, KafkaTemplate
- MySQL/PostgreSQL (JPA-compatible)
- WebClient for async calls
- Unit testing is not covered
---

## 📘 Flow Overview

1. **OrderController** receives a new order.
2. **OrderService**:
   - Locks product stock with `PESSIMISTIC_WRITE`
   - Deducts stock and saves the order
   - Serializes and stores `OutboxEvent` in the DB (same transaction)
3. **OutboxKafkaPublisher**:
   - Scheduled job picks unprocessed `ORDER_CREATED` events
   - Sends to Kafka topic `order-notifications`
   - Marks event as processed on success
4. **NotificationConsumer**:
   - Consumes from Kafka
   - Asynchronously calls `http://localhost:8080/notify`
   - On success, logs `NOTIFY_SUCCESS` event
   - On failure (3 retries), logs `NOTIFY_FAILED` event
5. **OutboxRetryScheduler**:
   - Scheduled job re-attempts failed notifications

---

##API

*POST* /order

```json
{
  "userId": 1,
  "productIds": [1, 2, 3]
}

This can be a other microservice as well
*POST* /notify

```json
{
  "request": "",
}

✅ Reliability
   - Outbox pattern ensures events are never lost, even if Kafka or /notify is down.
   - Kafka acts as a durable buffer, decoupling -  placeOrder from notification delivery.
   - /notify is retried asynchronously with capped retries and scheduled fallback via OutboxRetryScheduler.

✅ Performance
   - Async notification avoids blocking the main request.
   - Kafka handles massive throughput efficiently.
   - Retries are isolated from main user flow, keeping latency low.
   - frequent full table scans on the outbox_event table can degrade performance as it grows so create index on processed, eventType, and optionally createdAt and fetch events in batches 

✅ Low Latency
   - User request (/order) completes fast without waiting for /notify.
   - Kafka-based fanout is fast and scalable.

✅ Transactional Boundaries in Service A
- Transactional boundaries should encapsulate all DB operations in Service A. The REST call to Service B should be outside this boundary to avoid long-running transactions. Use a pattern like:

   - Begin DB transaction
   - Perform local DB operations (e.g., persist request metadata)
   - Commit transaction
   - Call Service B

- Based on Service B’s response:
   - Update status asynchronously
   - Use transactional outbox to ensure data       consistency as Service B’s call needs to trigger further state change.

✅ Threading Model and Implications
- Service A performs blocking I/O (e.g., REST to B), it may hold the thread. To optimize: Use WebClient (Reactor-based) for non-blocking I/O.

✅ Failure Scenarios
- Network Issues Between A and B
   a. Service B Unreachable:
   - Retry with exponential backoff
   - Circuit breaker to avoid cascading failures

   b. Timeouts/Lost Connections:
   - Set timeouts for WebClient
   - Log failure, update state to PENDING, and process later using a scheduled retry mechanism

(2) Service A Crash
   a. Possible Inconsistencies:
   - Crash before DB commit → no inconsistency
   - Crash after DB commit but before calling B → partial update
   - Crash after calling B but before response processed → uncertain final state

   b. Reconciliation Strategy:
   - Store intent or operation state (e.g., OUTBOX, PENDING)
   - On restart, a reconciliation job checks incomplete records and retries external calls or compensates
   - Use idempotent APIs between A and B to support safe reprocessing

✅ Sketch
Client --> Service A (Controller)
                   |
                   v
              Transactional Service
              | DB Commit |
                   |
            [Call Service B]
                   |
               Update Status
