# Order Service with Transactional Outbox and Kafka Integration

This Spring Boot project implements a **Transactional Outbox Pattern** for reliable event-driven communication. It ensures **strong consistency** between local database state and Kafka event publishing when placing an order.

---

## ✅ Features

- Order placement with product stock locking
- Transactional outbox for guaranteed event delivery
- Kafka publishing with retryable scheduled jobs
- Asynchronous downstream notifications
- Dead-letter handling for failed notifications

---

## 📦 Tech Stack

- Java 17+, Spring Boot
- JPA (MySQL/PostgreSQL)
- Kafka, KafkaTemplate
- WebClient (asynchronous HTTP)
- Docker & Docker Compose

---

## 📘 Flow Overview

1. **POST /order** receives new order request
2. `OrderService`:
   - Locks stock with `PESSIMISTIC_WRITE`
   - Saves the order & writes `OutboxEvent` in the same DB transaction
3. `OutboxKafkaPublisher` (Scheduled)
   - Picks unprocessed `ORDER_CREATED` events
   - Sends to Kafka (`order-notifications`)
   - Marks event as processed
4. `NotificationConsumer`:
   - Consumes Kafka events
   - Calls `POST /notify` asynchronously
   - On success → logs `NOTIFY_SUCCESS`
   - On failure (3 retries) → logs `NOTIFY_FAILED`
5. `OutboxRetryScheduler`:
   - Periodically retries failed notifications

---

## 🛠️ API Endpoints

### **POST** `/order`
```json
{
  "userId": 1,
  "productIds": [1, 2, 3]
}
```

### **POST** `/notify`
```json
{
  "request": ""
}
```

---

## 🔧 Non-Functional Requirements

### 1. Reliability
- Events never lost: outbox ensures message durability
- Kafka acts as buffer between order creation & notification
- Retries handled with fallback scheduler

### 2. Performance
- Notifications are async, keeping `/order` fast
- Kafka scales linearly
- Indexed `outbox_event` table avoids performance issues

### 3. Low Latency
- `/order` returns fast
- Kafka + async handling ensure minimal blocking

---

## 🔄 Transactional Boundaries in Service A

- Begin DB transaction
- Lock stock, persist order & outbox event
- Commit transaction
- Call Service B (e.g., notification)
- If needed, update status via async job (using outbox)

---

## 🧵 Threading Model

- Spring Boot (Tomcat): request per thread
- Use WebClient (non-blocking) for external calls
- Reactive chain recommended to avoid thread blocking

---

## 💥 Failure Scenarios

### 1. Network Issues (Service A ↔ Service B)

- **Unreachable B**:
  - Retry with exponential backoff
  - Circuit breaker to prevent overload

- **Timeouts**:
  - WebClient timeouts set
  - Update status to `PENDING` and retry

### 2. Service A Crash

- **Before DB Commit** → safe
- **After DB Commit, before Service B call** → outbox enables replay
- **After Service B call** → idempotent APIs and reconciliation jobs recover safely

### Reconciliation Strategy
- Use statuses like `PENDING`, `FAILED`
- Scheduler checks incomplete flows and reprocesses
- Idempotent external APIs ensure safe replays

---

## 📝 System Sketch

```
Client → [Service A: Controller]
                |
                ↓
     [Transactional Service Layer]
           | DB Commit |
                ↓
        [Call Service B (/notify)]
                ↓
         [Async Status Update]
```

