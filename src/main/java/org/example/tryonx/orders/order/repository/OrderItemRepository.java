package org.example.tryonx.orders.order.repository;


import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    Integer countByOrder(Order order);
    List<OrderItem> findByOrder(Order order);
    void deleteAllByMember(Member member);

}
