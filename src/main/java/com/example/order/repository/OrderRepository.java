package com.example.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
}