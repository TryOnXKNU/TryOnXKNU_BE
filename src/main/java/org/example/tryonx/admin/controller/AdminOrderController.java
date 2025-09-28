package org.example.tryonx.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.DeliveryStatusUpdateDto;
import org.example.tryonx.admin.dto.OrderInfoDto;
import org.example.tryonx.admin.dto.OrderListDto;
import org.example.tryonx.admin.service.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin Orders API", description = "관리자 주문 관리 API")
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "전체 주문 목록 조회")
    public ResponseEntity<List<OrderListDto>> getAllOrders() {
        List<OrderListDto> orders = adminOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세조회")
    public ResponseEntity<OrderInfoDto> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderId));
    }

    @PatchMapping("/{orderId}/delivery-status")
    @Operation(summary = "주문별 배송 상태 변경")
    public ResponseEntity<?> updateDeliveryStatus(
            @PathVariable("orderId") Integer orderId,
            @RequestBody DeliveryStatusUpdateDto dto
    ) {
        adminOrderService.updateDeliveryStatus(orderId, dto.getStatus());
        return ResponseEntity.ok().build();
    }

}
