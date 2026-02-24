package com.example.orderservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;
    private String aggregateId;
    private String type;

    private boolean processed = false;
    private LocalDateTime createdAt;

    public OutboxEvent(){}

    public OutboxEvent(String payload, String aggregateId, String type) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.processed = false;

    }

    public String getId() {
        return id;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
    public void markProcessed(){
        this.processed = true;
    }
}
