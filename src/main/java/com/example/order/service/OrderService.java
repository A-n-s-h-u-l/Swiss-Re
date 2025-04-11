package com.example.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order.domain.NotificationPayload;
import com.example.order.domain.Order;
import com.example.order.domain.OrderRequest;
import com.example.order.domain.OutboxEvent;
import com.example.order.domain.Product;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OutboxRepository;
import com.example.order.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void placeOrder(OrderRequest request) {
        for (Long productId : request.getProductIds()) {
            Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getStock() <= 0) {
                throw new RuntimeException("Out of stock");
            }
            product.setStock(product.getStock() - 1);
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductIds(request.getProductIds());
        orderRepository.save(order);

        try {
            String payload = objectMapper.writeValueAsString(new NotificationPayload(order.getId(), order.getUserId(), order.getProductIds()));
            outboxRepository.save(new OutboxEvent("ORDER_CREATED", payload, false));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
