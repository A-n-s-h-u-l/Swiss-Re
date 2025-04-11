package com.example.order.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class OutboxEvent {
    @Id @GeneratedValue private Long id;
    private String eventType;

    @Lob private String payload;
        
    private Instant createdAt = Instant.now();
    private boolean processed = false;

    public OutboxEvent(String eventType, String payload, boolean processed) {
        this.eventType = eventType;
        this.payload = payload;
        this.processed = processed;
    }

    public OutboxEvent() {}

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getPayload() {
        return payload;
    }
}
