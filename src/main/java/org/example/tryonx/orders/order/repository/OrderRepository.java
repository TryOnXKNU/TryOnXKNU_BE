package org.example.tryonx.orders.order.repository;

import org.example.tryonx.orders.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
