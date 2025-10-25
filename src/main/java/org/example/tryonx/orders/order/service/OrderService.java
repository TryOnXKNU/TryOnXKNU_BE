package org.example.tryonx.orders.order.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.cart.domain.CartItem;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.enums.*;
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
import org.example.tryonx.orders.order.dto.*;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.payment.domain.Payment;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.example.tryonx.returns.domain.Returns;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final PaymentRepository paymentRepository;
    private final ReturnRepository returnRepository;

    @Transactional
    public Integer createOrder(String email, OrderRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

        final boolean isFree = Boolean.TRUE.equals(requestDto.getFree());

        // 0) 결제 존재/상태 확인 및 멱등 처리 (FREE 아닐 때만)
        Payment pay = null;
        if (!isFree) {
            if (requestDto.getMerchantUid() == null || requestDto.getMerchantUid().isBlank()) {
                throw new IllegalArgumentException("merchantUid가 없습니다.");
            }
            pay = paymentRepository.findByMerchantUid(requestDto.getMerchantUid())
                    .orElseThrow(() -> new IllegalStateException("결제 정보가 없습니다. merchantUid=" + requestDto.getMerchantUid()));

            if (pay.getStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException("결제가 완료되지 않았습니다.");
            }
            if (pay.getOrder() != null) {
                // 멱등: 이미 주문 연결되어 있으면 그 주문 반환
                return pay.getOrder().getOrderId();
            }
        }


        // 1) 아이템 검증/재고 차감
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequestDto.Item item : requestDto.getItems()) {
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
                if (!Objects.equals(cartItem.getQuantity(), item.getQuantity())) {
                    throw new IllegalArgumentException("장바구니 항목의 수량과 요청 수량이 일치하지 않습니다.");
                }
            }

            int reqQty = item.getQuantity();
            if (productItem.getStock() < reqQty) {
                throw new IllegalStateException("재고가 부족합니다.");
            }

            // 재고는 한 번에 차감
            int newStock = productItem.getStock() - reqQty;
            productItem.setStock(newStock);

            // 재고 임계 알림 (필요시 로직 유지)
            if (newStock == 3) {
                List<CartItem> cartItems = cartItemRepository.findAll();
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

            // 수량만큼 OrderItem(수량=1) 생성
            for (int k = 0; k < reqQty; k++) {
                orderItems.add(
                        OrderItem.builder()
                                .member(member)
                                .productItem(productItem)
                                .quantity(1) // 항상 1
                                .price(product.getPrice())
                                .discountRate(product.getDiscountRate())
                                .build()
                );
            }
        }


        // 2) 금액 계산
        BigDecimal totalAmount = orderItems.stream()
                .map(i -> nz(i.getPrice()).multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = orderItems.stream()
                .map(i -> {
                    BigDecimal price = nz(i.getPrice());
                    BigDecimal rate = percentToRate(i.getDiscountRate()); // 10 -> 0.10
                    return price.multiply(rate).multiply(BigDecimal.valueOf(i.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        int usedPoints = requestDto.getPoint();
        if (usedPoints > member.getPoint()) {
            throw new IllegalArgumentException("사용 가능한 적립금을 초과했습니다.");
        }

        finalAmount = finalAmount.subtract(BigDecimal.valueOf(usedPoints));
        member.usePoint(usedPoints);

        //FREE가 아닐 때만 수행
        if (!isFree) {
            Integer paidAmountKrw = pay.getAmount(); // INT KRW
            System.out.println("결제 금액 : " + paidAmountKrw);
            System.out.println("주문 금액 : " + finalAmount.setScale(0, RoundingMode.DOWN).intValue());
            if (paidAmountKrw != null && paidAmountKrw.intValue() != finalAmount.setScale(0, RoundingMode.DOWN).intValue()) {
                throw new IllegalStateException("결제 금액과 주문 금액이 일치하지 않습니다.");
            }
        }

        String deliveryRequest = requestDto.getDeliveryRequest();

        // 3) 주문 생성
        Order order = Order.builder()
                .member(member)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount.max(BigDecimal.ZERO)) // 음수 방지
                .usedPoints(usedPoints)
                .orderedAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .deliveryRequest(deliveryRequest)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 주문번호 생성 후 저장
        String orderNum = generateOrderNum(savedOrder);
        savedOrder.setOrderNum(orderNum);
        orderRepository.save(savedOrder);

        // 4) 결제-주문 연결 및 상태 전환
        if (!isFree) {
            // 결제 주문 연결
            pay.setOrder(savedOrder);
            paymentRepository.save(pay);
        }
        // FREE 여부와 상관없이 결제 완료 상태로 간주(요구사항에 맞춰 바로 생성/완료)
        savedOrder.setStatus(OrderStatus.PAID);
        savedOrder.setDeliveryStatus(DeliveryStatus.ORDERED);
        orderRepository.save(savedOrder);

        // 5) 포인트 이력 처리
        String mainProductName = orderItems.get(0).getProductItem().getProduct().getProductName();
        int itemCount = orderItems.size();
        String productSummary = itemCount > 1 ? mainProductName + " 외 " + (itemCount - 1) + "건" : mainProductName;

        if (usedPoints >= 1000) {
            pointHistoryRepository.save(
                    PointHistory.use(member, usedPoints, "[" + productSummary + "] 주문 결제 중 적립금 사용으로 인한 차감"));
        }

        int savePoints = 0;
        if(usedPoints == 0){
            savePoints = savedOrder.getFinalAmount()
                    .multiply(BigDecimal.valueOf(0.01))
                    .setScale(0, RoundingMode.DOWN)
                    .intValue();
            member.savePoint(savePoints);
            memberRepository.save(member);
        }

        if (savePoints > 0) {
            pointHistoryRepository.save(
                    PointHistory.earn(member, savePoints, "[" + productSummary + "] 주문 적립 적립금 지급"));
        }

        Notification notification = Notification.builder()
                .member(member)
                .title("리뷰 작성 시 적립금 제공")
                .content("구매 상품 리뷰 작성 시 상품 구매 금액의 5% 적립금을 드려요!")
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

                                BigDecimal originalPrice = nz(product.getPrice());
//                                BigDecimal percent = nz(product.getDiscountRate());
                                BigDecimal percent = nz(item.getDiscountRate());

                                BigDecimal discountedByRate = calcDiscountPrice(originalPrice, percent);
                                BigDecimal itemPrice = nz(item.getPrice());

                                BigDecimal finalDiscountedUnitPrice;
                                if (itemPrice.signum() > 0 && percent.signum() > 0) {
                                    finalDiscountedUnitPrice = itemPrice.min(discountedByRate);
                                } else if (itemPrice.signum() > 0) {
                                    finalDiscountedUnitPrice = itemPrice;
                                } else {
                                    finalDiscountedUnitPrice = (percent.signum() > 0) ? discountedByRate : originalPrice;
                                }


                                return OrderItemDto.builder()
                                        .orderItemId(item.getOrderItemId())
                                        .productId(product.getProductId())
                                        .productName(product.getProductName())
                                        .size(productItem.getSize())
                                        .quantity(item.getQuantity())
                                        .imageUrl(productImage.getImageUrl())
                                        .price(originalPrice)                 // 원가
                                        .discountRate(percent)            // %
                                        .discountPrice(finalDiscountedUnitPrice) // 최종 할인가(적용가)
                                        .afterServiceStatus(item.getAfterServiceStatus())
                                        .build();
                            })
                            .toList();

                    // 첫 상품 기준 썸네일 이미지 가져오기 (대표 이미지)
                    String imageUrl = orderItems.stream()
                            .findFirst()
                            .map(OrderItem::getProductItem)
                            .map(ProductItem::getProduct)
                            .flatMap(product -> productImageRepository
                                    .findByProductAndIsThumbnailTrue(product)
                                    .map(ProductImage::getImageUrl))
                            .orElse(null);

                    BigDecimal price = orderItems.stream()
                            .findFirst()
                            .map(OrderItem::getProductItem)
                            .map(ProductItem::getProduct)
                            .map(Product::getPrice)
                            .orElse(BigDecimal.ZERO);

                    return new OrderListItem(
                            order.getOrderId(),
                            order.getMember().getMemberId(),
                            order.getOrderNum(),
                            orderItemDtos,
                            order.getFinalAmount(),
                            orderItemCount,
                            order.getOrderedAt(),
                            order.getDeliveryStatus(),
                            order.getStatus()
                    );
                })
                .toList();
    }

    // 유틸
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    // 숫자 % (예: 10) → 소수 (0.10)
    private static BigDecimal percentToRate(BigDecimal percent) {
        percent = nz(percent);
        if (percent.signum() <= 0) return BigDecimal.ZERO;
        return percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    // 할인가 = price * (1 - rate)
    private static BigDecimal calcDiscountPrice(BigDecimal price, BigDecimal percent) {
        price = nz(price);
        BigDecimal rate = percentToRate(percent);
        if (rate.signum() <= 0) return price; // 할인 없음
        return price.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(0, RoundingMode.HALF_UP); // 원단위 반올림
    }

    // "10%" 같은 표시용 문자열
    private static String toPercentString(BigDecimal percent) {
        percent = nz(percent);
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

//    public OrderDetailResponseDto getOrderDetail(Integer orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 주문 정보가 존재하지 않습니다."));
//        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 주문에 대한 결제 내역이 없습니다."));
//        Member member = order.getMember();
//        BigDecimal totalAmount = order.getTotalAmount();
//        BigDecimal finalAmount = order.getFinalAmount();
//        List<OrderItem> orderedItems = orderItemRepository.findByOrder(order);
//
//        String paymentMethod = null;
//        if(payment.getPgProvider().equals("kakaopay"))
//            paymentMethod = "카카오페이";
//        else if(payment.getPgProvider().equals("nice_v2"))
//            paymentMethod = payment.getCardName();
//
//        List<OrderDetailResponseDto.Item> items = orderedItems.stream().map(orderItem -> {
//            ProductItem productItem = orderItem.getProductItem();
//            Product product = productItem.getProduct();
//
//            BigDecimal price = nz(orderItem.getPrice());
//            BigDecimal percent = nz(orderItem.getDiscountRate()); // 10 = 10%
//            BigDecimal discountPrice = calcDiscountPrice(price, percent); // 최종 적용가
//            String discountRateStr = toPercentString(percent);            // "10%"
//
//            String imgUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
//                    .map(ProductImage::getImageUrl)
//                    .orElseGet(() -> product.getImages().isEmpty()
//                            ? null
//                            : product.getImages().get(0).getImageUrl());
//
//
//            return new OrderDetailResponseDto.Item(
//                    product.getProductId(),
//                    product.getProductName(),
//                    orderItem.getPrice(),
//                    orderItem.getQuantity(),
//                    productItem.getSize(),
//                    percent,
//                    discountPrice,
//                    productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl()
//            );
//        }).toList();
//
//        return new OrderDetailResponseDto(
//                order.getOrderId(),
//                order.getOrderNum(),
//                new MemberInfoDto(
//                        member.getName(),
//                        member.getPhoneNumber(),
//                        member.getAddress(),
//                        member.getPoint()
//                ),
//                totalAmount,
//                totalAmount.subtract(finalAmount),
//                finalAmount,
//                order.getStatus(),
//                order.getUsedPoints(),
//                items,
//                items.size(),
//                paymentMethod,
//                order.getOrderedAt(),
//                order.getDeliveryRequest(),
//                order.getDeliveryStatus()
//        );
//    }

public OrderDetailResponseDto getOrderDetail(Integer orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("해당 주문 정보가 존재하지 않습니다."));

    Member member = order.getMember();
    BigDecimal totalAmount = order.getTotalAmount();
    BigDecimal finalAmount = order.getFinalAmount();
    List<OrderItem> orderedItems = orderItemRepository.findByOrder(order);

    // 결제 존재 여부로 FREE 주문 판단
    boolean hasPayment = paymentRepository.existsByOrder_OrderId(orderId);

    String paymentMethod;
    Payment payment = null;
    if (!hasPayment) {
        // FREE 주문: 결제 레코드 없음
        paymentMethod = "전액 적립금 사용";
    } else {
        // 일반 결제 주문
        payment = paymentRepository.findByOrder_OrderId(orderId)
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

    List<OrderDetailResponseDto.Item> items = orderedItems.stream().map(orderItem -> {
        ProductItem productItem = orderItem.getProductItem();
        Product product = productItem.getProduct();

        BigDecimal price = nz(orderItem.getPrice());
        BigDecimal percent = nz(orderItem.getDiscountRate()); // 10 = 10%
        BigDecimal discountPrice = calcDiscountPrice(price, percent); // 최종 적용가

        String imgUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> product.getImages().isEmpty()
                        ? null
                        : product.getImages().get(0).getImageUrl());

        return new OrderDetailResponseDto.Item(
                orderItem.getOrderItemId(),
                product.getProductId(),
                product.getProductName(),
                orderItem.getPrice(),
                orderItem.getQuantity(),
                productItem.getSize(),
                percent,
                discountPrice,
                imgUrl,
                orderItem.getAfterServiceStatus()
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
            totalAmount.subtract(finalAmount), // 할인액
            finalAmount,
            order.getStatus(),
            order.getUsedPoints(),
            items,
            items.size(),
            paymentMethod,
            order.getOrderedAt(),
            order.getDeliveryRequest(),
            order.getDeliveryStatus()
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


//    public BigDecimal getTodaySalesAmount() {
//        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
//        LocalDateTime endOfDay = startOfDay.plusDays(1);
//
//        List<Order> todayOrders = orderRepository.findByOrderedAtBetween(startOfDay, endOfDay);
//
//        return todayOrders.stream()
//                .map(Order::getFinalAmount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }

    /** 오늘 매출 */
    public BigDecimal getTodaySalesAmount() {
        return getDailySalesAmount(LocalDate.now(), true);
    }

    /** 특정 일 매출 */
    public BigDecimal getDailySalesAmount(LocalDate date) {
        return getDailySalesAmount(date, true);
    }

    /** 특정 월 매출 */
    public BigDecimal getMonthlySalesAmount(YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end   = month.plusMonths(1).atDay(1).atStartOfDay();

        List<Order> orders = usePaidOnly()
                ? orderRepository.findByStatusInAndOrderedAtBetween(paidLikeStatuses(), start, end)
                : orderRepository.findByOrderedAtBetween(start, end);

        return sumFinalAmount(orders);
    }

    /** 전체 매출 */
    public BigDecimal getTotalSalesAmount() {
        List<Order> orders = usePaidOnly()
                ? orderRepository.findByStatusIn(paidLikeStatuses())
                : orderRepository.findAll();

        return sumFinalAmount(orders);
    }

    /** 특정 연 매출 */
    public BigDecimal getYearlySalesAmount(int year) {
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end   = LocalDate.of(year + 1, 1, 1).atStartOfDay();

        List<Order> orders = usePaidOnly()
                ? orderRepository.findByStatusInAndOrderedAtBetween(paidLikeStatuses(), start, end)
                : orderRepository.findByOrderedAtBetween(start, end);

        return sumFinalAmount(orders);
    }

    // ---- 내부 유틸 ----

    private BigDecimal getDailySalesAmount(LocalDate date, boolean paidOnly) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        List<Order> orders;

        if (paidOnly && hasStatusSupport()) {
            orders = orderRepository.findByStatusInAndOrderedAtBetween(
                    List.of(OrderStatus.PAID, OrderStatus.PARTIAL_REFUNDED),
                    start,
                    end
            );
        } else {
            orders = orderRepository.findByOrderedAtBetween(start, end);
        }

        return sumFinalAmount(orders);
    }

    private BigDecimal sumFinalAmount(List<Order> orders) {
        if (orders.isEmpty()) return BigDecimal.ZERO;

        //총 결제금액
        BigDecimal total = orders.stream()
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //환불된 주문들
        List<Integer> orderIds = orders.stream()
                .map(Order::getOrderId)
                .toList();

        List<Returns> completedReturns =
                returnRepository.findByStatusAndOrder_OrderIdIn(ReturnStatus.COMPLETED, orderIds);

        //환불 금액(할인 반영)
        BigDecimal refunded = completedReturns.stream()
                .map(r -> {
                    BigDecimal price = r.getPrice() != null ? r.getPrice() : BigDecimal.ZERO;
                    int qty = r.getQuantity() != null ? r.getQuantity() : 1;

                    //OrderItem에서 할인율 가져오기
                    BigDecimal discountRate = BigDecimal.ZERO;
                    if (r.getOrderItem() != null && r.getOrderItem().getDiscountRate() != null) {
                        discountRate = r.getOrderItem().getDiscountRate();
                    }

                    //할인 반영된 금액 계산
                    return calcDiscountPrice(price, discountRate)
                            .multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //총 매출 - 환불금액
        return total.subtract(refunded).max(BigDecimal.ZERO);
    }

    // ---- 설정 ----

    private boolean usePaidOnly() {
        return true; // 결제완료만 집계하려면 true
    }

    private boolean hasStatusSupport() {
        return true; // Order.status 필드가 enum으로 존재하니까 true
    }

    private OrderStatus paid() {
        return OrderStatus.PAID; // enum 상수 사용
    }

    private List<OrderStatus> paidLikeStatuses() {
        return List.of(OrderStatus.PAID, OrderStatus.PARTIAL_REFUNDED);
    }
}
