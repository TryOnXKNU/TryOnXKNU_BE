package org.example.tryonx.orders.order.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.cart.domain.CartItem;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.PointHistory;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.repository.PointHistoryRepository;
import org.example.tryonx.notice.domain.Notification;
import org.example.tryonx.notice.repository.NotificationRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.domain.OrderStatus;
import org.example.tryonx.orders.order.dto.*;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final NotificationRepository notificationRepository;
    private final PointHistoryRepository pointHistoryRepository;


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

                    if (item.getCartItemId() != null) {
                        CartItem cartItem = cartItemRepository.findById(item.getCartItemId())
                                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목이 존재하지 않습니다."));

                        if (!cartItem.getMember().equals(member)) {
                            throw new IllegalArgumentException("본인의 장바구니 항목만 주문할 수 있습니다.");
                        }

                        if (!cartItem.getProductItem().equals(productItem)) {
                            throw new IllegalArgumentException("장바구니 항목의 상품 정보가 일치하지 않습니다.");
                        }

                        if (!cartItem.getQuantity().equals(item.getQuantity())) {
                            throw new IllegalArgumentException("장바구니 항목의 수량과 요청 수량이 일치하지 않습니다.");
                        }
                    }

                    if (productItem.getStock() < item.getQuantity()) {
                        throw new IllegalStateException("재고가 부족합니다.");
                    }

                    int newStock = productItem.getStock() - item.getQuantity();
                    productItem.setStock(newStock);

                    if (newStock == 3 ) {
                        List<CartItem> cartItems = cartItemRepository.findAll(); // 또는 cartItemRepository.findByProductItem(productItem) 권장
                        for (CartItem cartItem : cartItems) {
                            if (cartItem.getProductItem().equals(productItem)) {
                                Member target = cartItem.getMember();
                                String productName = productItem.getProduct().getProductName();
                                String size = productItem.getSize().name();
                                String content = "[재고 알림] " + productName + " (" + size + ") 상품의 재고가 " + newStock + "개 남았습니다. 서두르세요!";

                                Notification notification = Notification.builder()
                                        .member(target)
                                        .title("재고 알림")
                                        .content(content)
                                        .build();
                                notificationRepository.save(notification);
                            }
                        }
                    }

                    if (newStock == 0) {
                        productItem.setStatus(ProductStatus.SOLDOUT);
                    }
                    productItemRepository.save(productItem);

                    return OrderItem.builder()
                            .member(member)
                            .productItem(productItem)
                            .quantity(item.getQuantity())
                            .price(product.getPrice())
                            .discountRate(product.getDiscountRate())
                            .build();
                })
                .collect(Collectors.toList());


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
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .usedPoints(usedPoints)
                .orderedAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        String orderNum = generateOrderNum(savedOrder);
        savedOrder.setOrderNum(orderNum);
        orderRepository.save(savedOrder);

        // 포인트 사용/적립 내역 저장
        String mainProductName = orderItems.get(0).getProductItem().getProduct().getProductName();
        int itemCount = orderItems.size();
        String productSummary = itemCount > 1 ? mainProductName + " 외 " + (itemCount - 1) + "건" : mainProductName;

        if (usedPoints > 0) {
            pointHistoryRepository.save(PointHistory.use(member, usedPoints, "[" + productSummary + "] 주문 결제 중 포인트 사용으로 인한 차감"));
        }

        int savePoints = finalAmount.multiply(BigDecimal.valueOf(0.01))
                .setScale(0, BigDecimal.ROUND_DOWN)
                .intValue();

        if (savePoints > 0) {
            pointHistoryRepository.save(PointHistory.earn(member, savePoints, "[" + productSummary + "] 주문 적립 포인트 지급"));
        }


        Notification notification = Notification.builder()
                .member(member)
                .title("리뷰 작성 시 포인트 제공")
                .content("구매 상품 리뷰 작성 시 10% 포인트를 드려요!")
                .build();

        notificationRepository.save(notification);

        return savedOrder.getOrderId();
    }



    //orderNum 자동 생성
    private String generateOrderNum(Order order) {
        LocalDateTime orderedAt = order.getOrderedAt();
        String datePart = orderedAt.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String idPart = String.format("%04d", order.getOrderId());
        return datePart + "00" + idPart;
    }

    public List<OrderListItem> getMyOrders(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 회원이 없습니다."));
        List<Order> orders = orderRepository.findByMember(member);

        return orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = order.getOrderItems();
                    int orderItemCount = orderItems.size();

                    List<OrderItemDto> orderItemDtos = orderItems.stream()
                            .map(item -> {
                                ProductItem productItem = item.getProductItem();
                                Product product = productItem.getProduct();
                                ProductImage productImage = productImageRepository.findByProductAndIsThumbnailTrue(product)
                                        .orElseThrow(() -> new EntityNotFoundException("상품 이미지가 없습니다."));
                                return new OrderItemDto(
                                        item.getOrderItemId(),
                                        product.getProductName(),
                                        productItem.getSize(),
                                        product.getPrice().multiply(product.getDiscountRate().divide(BigDecimal.valueOf(100))),
                                        item.getQuantity(),
                                        productImage.getImageUrl()
                                );
                            }).toList();

                    // 첫 상품 기준 썸네일 이미지 가져오기 (대표 이미지)
                    String imageUrl = orderItems.stream()
                            .findFirst()
                            .map(OrderItem::getProductItem)
                            .map(ProductItem::getProduct)
                            .flatMap(product -> productImageRepository
                                    .findByProductAndIsThumbnailTrue(product)
                                    .map(ProductImage::getImageUrl))
                            .orElse(null);

                    return new OrderListItem(
                            order.getOrderId(),
                            order.getMember().getMemberId(),
                            order.getOrderNum(),
                            orderItemDtos,
                            order.getFinalAmount(),
                            orderItemCount,
                            order.getOrderedAt()
                    );
                })
                .toList();
    }
    public OrderDetailResponseDto getOrderDetail(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문 정보가 존재하지 않습니다."));

        Member member = order.getMember();
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal finalAmount = order.getFinalAmount();
        List<OrderItem> orderedItems = orderItemRepository.findByOrder(order);

        List<OrderDetailResponseDto.Item> items = orderedItems.stream().map(orderItem -> {
            ProductItem productItem = orderItem.getProductItem();
            Product product = productItem.getProduct();

            return new OrderDetailResponseDto.Item(
                    product.getProductName(),
                    orderItem.getPrice(),
                    orderItem.getQuantity(),
                    productItem.getSize(),
                    orderItem.getDiscountRate().toPlainString(),
                    productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl()
            );
        }).toList();

        return new OrderDetailResponseDto(
                order.getOrderId(),
                order.getOrderNum(),
                new MemberInfoDto(
                        member.getName(),
                        member.getPhoneNumber(),
                        member.getAddress(),
                        member.getPoint()
                ),
                totalAmount,
                totalAmount.subtract(finalAmount),
                finalAmount,
                order.getStatus(),
                order.getUsedPoints(),
                items,
                items.size(),
                order.getOrderedAt()
        );
    }

    public Integer countMyOrders(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 회원이 없습니다."));
        return orderRepository.countByMember(member);
    }

    public long countAllOrders() {
        return orderRepository.count();
    }

}
