package org.example.tryonx.orders.order.repository;


import org.example.tryonx.orders.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}
