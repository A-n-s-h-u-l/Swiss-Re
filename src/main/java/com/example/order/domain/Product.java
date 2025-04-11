package com.example.order.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Product {
    @Id private Long id;
    private String name;
    private int stock;

    public Product() {}

    public Product(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }
}
