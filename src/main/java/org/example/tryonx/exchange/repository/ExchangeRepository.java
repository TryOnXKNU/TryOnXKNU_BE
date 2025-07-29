package org.example.tryonx.exchange.repository;

import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<Exchange, Integer> {
    List<Exchange> findAllByMember(Member member);
    void deleteAllByMember(Member member);
    List<Exchange> findAllByStatus(ExchangeStatus status);
    Optional<Exchange> findByOrderItem(OrderItem orderItem);
}
