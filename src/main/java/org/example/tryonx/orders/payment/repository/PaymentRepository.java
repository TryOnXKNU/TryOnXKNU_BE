package org.example.tryonx.orders.payment.repository;

import org.example.tryonx.orders.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByMerchantUid(String merchantUid);
    Optional<Payment> findByImpUid(String impUid);
    boolean existsByMerchantUid(String merchantUid);

    // 주문별 결제 조회가 필요할 때
    Optional<Payment> findByOrder_OrderId(Integer orderId);
}