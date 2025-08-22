package org.example.tryonx.orders.order.repository;

import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByMember(Member member);
    List<Order> findByMemberEmail(String email);
    Integer countByMember(Member member);
    void deleteAllByMember(Member member);
    List<Order> findByOrderedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndOrderedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);
}
