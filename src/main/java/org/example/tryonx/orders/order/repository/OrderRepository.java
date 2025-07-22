package org.example.tryonx.orders.order.repository;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByMember(Member member);
    List<Order> findByMemberEmail(String email);
    Integer countByMember(Member member);
    void deleteAllByMember(Member member);
}
