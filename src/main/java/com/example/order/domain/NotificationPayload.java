package com.example.order.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class NotificationPayload {
    private Long orderId;
    private Long userId;
    private List<Long> productIds;
}
