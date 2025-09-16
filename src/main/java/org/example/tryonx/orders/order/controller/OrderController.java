package org.example.tryonx.orders.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.orders.order.dto.*;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.order.service.DeliveryService;
import org.example.tryonx.orders.order.service.OrderPreviewService;
import org.example.tryonx.orders.order.service.OrderService;
import org.example.tryonx.orders.payment.domain.Payment;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderPreviewService orderPreviewService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;


    @PostMapping("/preview")
    public ResponseEntity<OrderPreviewResponseDto> previewOrder(
            @RequestBody OrderPreviewRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        String email = userDetails.getUsername();
        OrderPreviewResponseDto response = orderPreviewService.calculatePreview(email, requestDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDto req,
                                         @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        final boolean isFree = Boolean.TRUE.equals(req.getFree());

        Payment pay = null;
        // 1) 결제 존재/상태 확인 — FREE가 아닐 때만
        if (!isFree) {
            if (req.getMerchantUid() == null || req.getMerchantUid().isBlank()) {
                return ResponseEntity.badRequest().body("merchantUid가 없습니다.");
            }

            pay = paymentRepository.findByMerchantUid(req.getMerchantUid())
                    .orElseThrow(() -> new IllegalStateException("결제 정보가 없습니다. merchantUid=" + req.getMerchantUid()));

            if (pay.getStatus() != PaymentStatus.PAID) {
                return ResponseEntity.badRequest().body("결제가 완료되지 않았습니다.");
            }

            // 2) 멱등 처리 — 이미 주문 연결돼 있으면 그 주문 ID 반환
            if (pay.getOrder() != null) {
                return ResponseEntity.ok(new OrderCreateResponse(pay.getOrder().getOrderId()));
            }
        }

        // 3) 주문 생성 — FREE 여부와 무관
        Integer orderId = orderService.createOrder(user.getUsername(), req);

        // 4) 결제-주문 연결 및 상태 갱신 — FREE가 아닐 때만
        if (!isFree) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            // 결제-주문 연결
            pay.setOrder(order);
            paymentRepository.save(pay);

            // 정책에 따라 주문 상태 갱신 (서비스에서 이미 처리한다면 이 부분은 생략 가능)
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }

        return ResponseEntity.ok(new OrderCreateResponse(orderId));
    }


    record OrderCreateResponse(Integer orderId) {}


    @GetMapping("/my")
    public ResponseEntity<List<OrderListItem>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<OrderListItem> orders = orderService.getMyOrders(email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getOrder(@PathVariable Integer orderId) {
        OrderDetailResponseDto orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping("/my/count")
    public ResponseEntity<Integer> getMyCount(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Integer countMyOrders = orderService.countMyOrders(email);
        return ResponseEntity.ok(countMyOrders);
    }

    // 특정 주문의 배송 이력 조회
    @GetMapping("/delivery/{orderId}")
    public List<DeliveryHistoryDto> getDeliveryHistory(
            @PathVariable Integer orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        return deliveryService.getDeliveryHistory(orderId, email);
    }
}
