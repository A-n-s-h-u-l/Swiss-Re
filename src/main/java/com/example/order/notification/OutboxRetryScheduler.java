package com.example.order.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.order.domain.OutboxEvent;
import com.example.order.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxRetryScheduler {
    private final OutboxRepository outboxRepository;
    private final NotificationConsumer notificationConsumer;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedNotifications() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<OutboxEvent> failed = outboxRepository.findByProcessedFalseAndEventType("NOTIFY_FAILED", pageable);
        for (OutboxEvent event : failed.getContent()) {
            notificationConsumer.sendAsyncNotify(event.getPayload(), 0);
        }
    }
}
