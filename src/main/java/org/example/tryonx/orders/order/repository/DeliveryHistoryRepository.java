package org.example.tryonx.orders.order.repository;

import org.example.tryonx.orders.order.domain.DeliveryHistory;
import org.example.tryonx.orders.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryHistoryRepository extends JpaRepository<DeliveryHistory, Long> {
    List<DeliveryHistory> findByOrder_OrderIdOrderByChangedAtDesc(Integer orderId);
    Optional<DeliveryHistory> findTopByOrderOrderByChangedAtDesc(Order order);

}
