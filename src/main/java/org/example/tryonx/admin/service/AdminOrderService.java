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
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.payment.domain.Payment;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;


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

    public OrderInfoDto getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문이 존재하지 않습니다."));

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        List<OrderInfoItemDto> itemDtos = orderItems.stream().map(orderItem -> {
            var productItem = orderItem.getProductItem();
            var product = productItem.getProduct();
            String imageUrl = product.getImages().stream()
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(null);

            return OrderInfoItemDto.builder()
                    .orderItemId(orderItem.getOrderItemId())
                    .productName(product.getProductName())
                    .imageUrl(imageUrl)
                    .size(productItem.getSize())
                    .quantity(orderItem.getQuantity())
                    .price(orderItem.getPrice())
                    .build();
        }).toList();

        Member member = order.getMember();
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문에 대한 결제내역이 없습니다.."));
        return OrderInfoDto.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getStatus())
                .orderAt(order.getOrderedAt())
                .orderNum(order.getOrderNum())
                .name(member.getName())
                .profileUrl(member.getProfileUrl())
                .phoneNumber(member.getPhoneNumber())
                .email(member.getEmail())
                .address(order.getMember().getAddress())
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
                .paymentMethod(payment.getCardName())
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

        order.setDeliveryStatus(newStatus);
    }
}
