package org.example.tryonx.orders.order.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.orders.order.dto.*;
import org.example.tryonx.orders.order.service.OrderPreviewService;
import org.example.tryonx.orders.order.service.OrderService;
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
    public ResponseEntity<Integer> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OrderRequestDto requestDto
    ) {
        String email = userDetails.getUsername();
        Integer orderId = orderService.createOrder(email, requestDto);
        return ResponseEntity.ok(orderId);
    }

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
