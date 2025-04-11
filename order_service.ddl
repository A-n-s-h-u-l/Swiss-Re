CREATE DATABASE IF NOT EXISTS order_service;
USE order_service;

-- Product table
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    stock INT NOT NULL
);

-- Order table
CREATE TABLE IF NOT EXISTS customer_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_ids TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ORDER_PRODUCT mapping
CREATE TABLE order_product (
    order_id BIGINT,
    product_id BIGINT,
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES customer_order(id)
);

-- Outbox event table
CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Index on outbox_event to fetch events optimally
CREATE INDEX idx_outbox_processed_eventtype_createdat
ON outbox_event (processed, event_type, created_at);

-- Sample products
INSERT INTO product (name, stock) VALUES 
('Phone', 10),
('Laptop', 5),
('Headphones', 15);
