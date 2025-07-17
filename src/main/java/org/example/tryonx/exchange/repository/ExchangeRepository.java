package org.example.tryonx.exchange.repository;

import org.example.tryonx.exchange.domain.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, Integer> {
}
