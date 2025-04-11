package com.example.order.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.order.domain.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByProcessedFalseAndEventType(String eventType);

    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.eventType = :eventType ORDER BY e.createdAt ASC")
    Page<OutboxEvent> findByProcessedFalseAndEventType(@Param("eventType") String eventType, Pageable pageable);
}
