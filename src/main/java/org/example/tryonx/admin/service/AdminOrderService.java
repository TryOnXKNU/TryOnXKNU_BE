package org.example.tryonx.admin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.OrderInfoDto;
import org.example.tryonx.admin.dto.OrderInfoItemDto;
import org.example.tryonx.admin.dto.OrderItemsDto;
import org.example.tryonx.admin.dto.OrderListDto;
import org.example.tryonx.enums.DeliveryStatus;
import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.DeliveryHistory;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.DeliveryHistoryRepository;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.payment.domain.Payment;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;

    @Transactional
    public List<OrderListDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> {
                    List<OrderItemsDto> items = order.getOrderItems().stream()
                            .map(item -> OrderItemsDto.builder()
                                    .orderItemId(item.getOrderItemId())
                                    .productName(item.getProductItem().getProduct().getProductName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList());

                    return OrderListDto.builder()
                            .orderId(order.getOrderId())
                            .orderNum(order.getOrderNum())
                            .orderAt(order.getOrderedAt())
                            .orderStatus(order.getStatus())
                            .items(items)
                            .deliveryStatus(order.getDeliveryStatus())
                            .totalPrice(order.getFinalAmount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // null-safe
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    public OrderInfoDto getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문이 존재하지 않습니다."));

        boolean hasPayment = paymentRepository.existsByOrder_OrderId(orderId);
        String paymentMethod;

        if (!hasPayment) {
            paymentMethod = "전액 적립금 사용";
        }else {
            // 일반 결제 주문
            Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 주문에 대한 결제 내역이 없습니다."));

            String pg = payment.getPgProvider();
            if ("kakaopay".equalsIgnoreCase(pg)) {
                paymentMethod = "카카오페이";
            } else if ("nice_v2".equalsIgnoreCase(pg)) {
                paymentMethod = payment.getCardName();
            } else {
                paymentMethod = pg;
            }
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        List<OrderInfoItemDto> itemDtos = orderItems.stream().map(orderItem -> {
            var productItem = orderItem.getProductItem();
            var product = productItem.getProduct();

            // 썸네일 이미지(없으면 첫 번째 이미지, 그것도 없으면 null)
            String imageUrl = product.getImages().stream()
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(null);

            // 주문 시점 스냅샷 필드만 사용
            BigDecimal unitPrice = nz(orderItem.getPrice());          // 주문 당시 단가
            BigDecimal rate = nz(orderItem.getDiscountRate());        // 주문 당시 할인율(예: 10 -> 10%)
            BigDecimal qty = BigDecimal.valueOf(orderItem.getQuantity());

            // 최종단가(단품) = 단가 * (1 - rate/100)   → 금액 라운딩
            BigDecimal finalUnitPrice = unitPrice.multiply(
                    BigDecimal.ONE.subtract(
                            rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    )
            ).setScale(0, RoundingMode.DOWN);

            // 아이템별 최종 금액 = 최종단가 * 수량
            BigDecimal discountedFinal = finalUnitPrice.multiply(qty).setScale(0, RoundingMode.DOWN);

            return OrderInfoItemDto.builder()
                    .orderItemId(orderItem.getOrderItemId())
                    .productName(product.getProductName())
                    .imageUrl(imageUrl)
                    .size(productItem.getSize())
                    .quantity(orderItem.getQuantity())
                    .price(unitPrice)            // 스냅샷 단가
                    .discountRate(rate)          // 스냅샷 할인율(%)
                    .discountPrice(discountedFinal)
                    .afterServiceStatus(orderItem.getAfterServiceStatus())
                    .build();
        }).toList();

        Member member = order.getMember();
        return OrderInfoDto.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getStatus())
                .orderAt(order.getOrderedAt())
                .orderNum(order.getOrderNum())
                .name(member.getName())
                .profileUrl(member.getProfileUrl())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .address(member.getAddress())
                .totalPrice(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .usedPoints(order.getUsedPoints())
                .deliveryFee("무료배송")
                .finalAmount(order.getFinalAmount())
                .discountRate(
                        order.getDiscountAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                                ? order.getDiscountAmount()
                                .divide(order.getTotalAmount(), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO
                )
                .paymentMethod(paymentMethod)
                .items(itemDtos)
                .deliveryStatus(order.getDeliveryStatus())
                .build();
    }

    @Transactional
    public void updateDeliveryStatus(Integer orderId, DeliveryStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문 없음"));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 배송 상태 변경 가능");
        }

        // 최근 배송 이력 가져오기
        DeliveryHistory lastHistory = deliveryHistoryRepository
                .findTopByOrderOrderByChangedAtDesc(order)
                .orElse(null);

        // 가장 최근 상태가 동일하면 새 기록 생성 X
        if (lastHistory != null && lastHistory.getDeliveryStatus() == newStatus) {
            return;
        }

        // 실제 배송 상태 변경
        order.setDeliveryStatus(newStatus);

        // 새로운 이력 저장
        DeliveryHistory history = DeliveryHistory.builder()
                .order(order)
                .deliveryStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .build();

        deliveryHistoryRepository.save(history);
    }

}
