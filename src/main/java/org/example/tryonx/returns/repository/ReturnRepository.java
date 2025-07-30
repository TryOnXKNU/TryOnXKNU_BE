package org.example.tryonx.returns.repository;

import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.returns.domain.Returns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRepository extends JpaRepository<Returns, Integer> {
    List<Returns> findAllByMember(Member member);
    void deleteAllByMember(Member member);
    List<Returns> findAllByStatus(ReturnStatus status);
    Optional<Returns> findByOrderItem(OrderItem orderItem);
    void deleteByOrderItem(OrderItem orderItem);
}
