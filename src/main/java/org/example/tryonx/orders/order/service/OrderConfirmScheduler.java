package org.example.tryonx.orders.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.DeliveryStatus;
import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.PointHistory;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.repository.PointHistoryRepository;
import org.example.tryonx.orders.order.domain.DeliveryHistory;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.repository.DeliveryHistoryRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Transactional
@RequiredArgsConstructor
public class OrderConfirmScheduler {

    private final OrderRepository orderRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 1분마다 확인
    @Scheduled(fixedDelay = 60_000)
    public void autoConfirmOrders() {
        // 배송완료인데 아직 CONFIRMED 되지 않은 주문들 찾기
        List<Order> deliveredOrders = orderRepository.findByDeliveryStatus(DeliveryStatus.DELIVERED);

        for (Order order : deliveredOrders) {

            if (order.getStatus() == OrderStatus.CONFIRMED) continue;

            DeliveryHistory latest = deliveryHistoryRepository
                    .findTopByOrderOrderByChangedAtDesc(order)
                    .orElse(null);

            if (latest == null) continue;

            // DELIVERED 아닌 이력은 무시
            if (latest.getDeliveryStatus() != DeliveryStatus.DELIVERED) continue;

            // 배송완료 후  7일 경과 여부
            if (latest.getChangedAt().plusDays(7).isAfter(LocalDateTime.now())) {
                continue;
            }

            // ---- 주문 CONFIRMED 처리 ----
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // ---- 포인트 적립 ----
            if (order.getUsedPoints() == 0) {

                Member member = order.getMember();
                int savePoints = order.getFinalAmount()
                        .multiply(BigDecimal.valueOf(0.01))
                        .setScale(0, RoundingMode.DOWN)
                        .intValue();

                member.savePoint(savePoints);
                memberRepository.save(member);

                pointHistoryRepository.save(
                        PointHistory.earn(member, savePoints,
                                "주문 확정에 의한 1% 적립금 지급")
                );
            }
        }
    }
}
