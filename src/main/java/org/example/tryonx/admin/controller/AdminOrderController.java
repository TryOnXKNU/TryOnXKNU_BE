package org.example.tryonx.admin.controller;

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
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<List<OrderListDto>> getAllOrders() {
        List<OrderListDto> orders = adminOrderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderInfoDto> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetail(orderId));
    }

    @PatchMapping("/{orderId}/delivery-status")
    public ResponseEntity<?> updateDeliveryStatus(
            @PathVariable("orderId") Integer orderId,
            @RequestBody DeliveryStatusUpdateDto dto
    ) {
        adminOrderService.updateDeliveryStatus(orderId, dto.getStatus());
        return ResponseEntity.ok().build();
    }

}
