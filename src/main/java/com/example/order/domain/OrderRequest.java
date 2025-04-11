package com.example.order.domain;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long userId;
    private List<Long> productIds;
}
