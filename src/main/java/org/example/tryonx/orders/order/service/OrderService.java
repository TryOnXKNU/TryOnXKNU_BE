package org.example.tryonx.orders.order.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.domain.OrderStatus;
import org.example.tryonx.orders.order.dto.OrderListItem;
import org.example.tryonx.orders.order.dto.OrderRequestDto;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;

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

        if (usedPoints > member.getPoint()) {
            throw new IllegalArgumentException("사용 가능한 포인트를 초과했습니다.");
        }

        finalAmount = finalAmount.subtract(BigDecimal.valueOf(usedPoints));
        member.usePoint(usedPoints);

        Order order = Order.builder()
                .member(member)
                .totalAmount(totalAmount)
                .finalAmount(finalAmount)
                .usedPoints(usedPoints)
                .orderedAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);
        memberRepository.save(member);
        Order savedOrder = orderRepository.save(order);
        return savedOrder.getOrderId();
    }

    public List<OrderListItem> getMyOrders(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 회원이 없습니다."));
        List<Order> orders = orderRepository.findByMember(member);

        return orders.stream()
                .flatMap(order -> {
                    List<OrderItem> orderItems = order.getOrderItems();
                    int orderItemCount = orderItems.size();
                    return orderItems.stream()
                            .findFirst()
                            .map(firstItem -> {
                                ProductItem productItem = firstItem.getProductItem();
                                Product product = productItem.getProduct();


                                return Stream.of(new OrderListItem(
                                        String.valueOf(order.getOrderId()),
                                        product.getProductName(),
                                        productItem.getSize(),
                                        firstItem.getQuantity(),
                                        firstItem.getPrice(),
                                        productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl(),
                                        orderItemCount
                                ));
                            }).orElseGet(Stream::empty);
                })
                .toList();
    }

}
