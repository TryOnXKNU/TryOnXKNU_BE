package org.example.tryonx.orders.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.orders.order.dto.*;
import org.example.tryonx.orders.order.repository.OrderRepository;
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
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        // 1) 결제 존재/상태 확인
        Payment pay = paymentRepository.findByMerchantUid(req.getMerchantUid())
                .orElseThrow(() -> new IllegalStateException("결제 정보가 없습니다. merchantUid=" + req.getMerchantUid()));
        if (pay.getStatus() != PaymentStatus.PAID) {
            return ResponseEntity.badRequest().body("결제가 완료되지 않았습니다.");
        }

        // 2) 멱등 처리: 이미 주문에 연결돼 있으면 그 주문 ID 그대로 반환
        if (pay.getOrder() != null) {
            return ResponseEntity.ok(new OrderCreateResponse(pay.getOrder().getOrderId()));
        }

        // 3) 주문 생성
        Integer orderId = orderService.createOrder(user.getUsername(), req);
        Order order = orderRepository.findById(orderId).orElseThrow();

        // 4) 결제-주문 연결
        pay.setOrder(order);
        paymentRepository.save(pay);

        // 정책에 따라 주문 상태 갱신
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

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
}
