package org.example.tryonx.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.OrderListDto;
import org.example.tryonx.admin.service.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
