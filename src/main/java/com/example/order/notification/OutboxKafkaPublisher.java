package com.example.order.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.example.order.domain.OutboxEvent;
import com.example.order.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @SuppressWarnings("removal")
    @Scheduled(fixedDelay = 10000)
    public void publishUnsentEvents() {
        Pageable pageable = PageRequest.of(0, 100);
        Page<OutboxEvent> page = outboxRepository.findByProcessedFalseAndEventType("ORDER_CREATED", pageable);
        for (OutboxEvent event : page.getContent()) {
            ListenableFuture<SendResult<String, String>> future = (ListenableFuture<SendResult<String, String>>) kafkaTemplate.send("order-notifications", event.getPayload());

            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    event.setProcessed(true);
                    outboxRepository.save(event);
                }

                @Override
                public void onFailure(Throwable ex) {
                    // log and leave for retry
                }
            });
        }
    }
}