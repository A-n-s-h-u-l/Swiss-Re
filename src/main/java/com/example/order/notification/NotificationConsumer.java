package com.example.order.notification;

import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.order.domain.OutboxEvent;
import com.example.order.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class NotificationConsumer<webClientBuilder> {
    private final OutboxRepository outboxRepository;
    private final WebClient.Builder webClientBuilder;

    @KafkaListener(topics = "order-notifications", groupId = "notification-group")
    public void consume(String message) {
        sendAsyncNotify(message, 0);
    }

    public void sendAsyncNotify(String message, int attempt) {
        webClientBuilder.build()
            .post()
            .uri("http://localhost:8080/notify")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(response -> {
                outboxRepository.save(new OutboxEvent("NOTIFY_SUCCESS", message, true));
            })
            .doOnError(error -> {
                if (attempt < 3) {
                    try {
                        Thread.sleep(1000L * (attempt + 1));
                    } catch (InterruptedException ignored) {}
                    sendAsyncNotify(message, attempt + 1);
                } else {
                    outboxRepository.save(new OutboxEvent("NOTIFY_FAILED", message, false));
                }
            })
            .subscribe();
    }
}
