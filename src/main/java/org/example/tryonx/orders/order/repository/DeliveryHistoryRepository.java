package org.example.tryonx.orders.order.repository;

import org.example.tryonx.orders.order.domain.DeliveryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {
    List<DeliveryHistory> findByOrder_OrderIdOrderByChangedAtDesc(Integer orderId);
}
