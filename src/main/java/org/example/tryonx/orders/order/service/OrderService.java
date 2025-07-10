package org.example.tryonx.orders.order.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.dto.OrderRequestDto;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;

    @Transactional
    public Integer createOrder(String email, OrderRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

        List<OrderItem> orderItems = requestDto.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("상품 정보 없음"));

                    ProductItem productItem = productItemRepository.findByProductAndSize(product, item.getSize())
                            .orElseThrow(() -> new EntityNotFoundException("사이즈 정보 없음"));

                    BigDecimal itemPrice = product.getPrice();
                    BigDecimal discountRate = product.getDiscountRate();

                    return OrderItem.builder()
                            .productItem(productItem)
                            .quantity(item.getQuantity())
                            .price(itemPrice)
                            .discountRate(discountRate)
                            .build();
                })
                .toList();

        BigDecimal totalAmount = orderItems.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = orderItems.stream()
                .map(i -> i.getPrice()
                        .multiply(i.getDiscountRate().divide(BigDecimal.valueOf(100)))
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        int usedPoints = requestDto.getPoint();

        // 포인트 차감
        if (usedPoints > member.getPoint()) {
            throw new IllegalArgumentException("사용 가능한 포인트를 초과했습니다.");
        }

        finalAmount = finalAmount.subtract(BigDecimal.valueOf(usedPoints));
        member.usePoint(usedPoints); // Member 엔티티에 포인트 차감 메소드 필요

        Order order = Order.builder()
                .member(member)
                .totalAmount(totalAmount)
                .finalAmount(finalAmount)
                .usedPoints(usedPoints)
                .build();

        // 연관관계 설정
        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);
        memberRepository.save(member);
        Order savedOrder = orderRepository.save(order);
        return savedOrder.getOrderId();
    }
}
