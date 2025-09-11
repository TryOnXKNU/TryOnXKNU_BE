package org.example.tryonx.orders.order.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.dto.DeliveryHistoryDto;
import org.example.tryonx.orders.order.repository.DeliveryHistoryRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;


    public List<DeliveryHistoryDto> getDeliveryHistory(Integer orderId, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다. id=" + orderId));

        if (!order.getMember().getMemberId().equals(member.getMemberId())) {
            throw new SecurityException("본인 주문만 조회할 수 있습니다.");
        }

        return deliveryHistoryRepository.findByOrder_OrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(h -> new DeliveryHistoryDto(
                        h.getOrder().getOrderId(),
                        h.getChangedAt(),
                        h.getDeliveryStatus()
                ))
                .collect(Collectors.toList());
    }

}
