package org.example.tryonx.exchange.repository;

import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRepository extends JpaRepository<Exchange, Integer> {
    List<Exchange> findAllByMember(Member member);
    void deleteAllByMember(Member member);
}
