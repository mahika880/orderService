package com.example.orderservice.repository;

import com.example.orderservice.model.Order;

public interface OrderRepository {
    void save(Order order);

    Order findById(String orderId);
}
