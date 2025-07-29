package org.example.tryonx.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.OrderItemsDto;
import org.example.tryonx.admin.dto.OrderListDto;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.returns.domain.Returns;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final OrderRepository orderRepository;

    public List<OrderListDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> {
                    List<OrderItemsDto> items = order.getOrderItems().stream()
                            .map(item -> OrderItemsDto.builder()
                                    .orderItemId(item.getOrderItemId())
                                    .productName(item.getProductItem().getProduct().getProductName())
                                    .price(item.getPrice())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList());

                    return OrderListDto.builder()
                            .orderId(order.getOrderId())
                            .orderNum(order.getOrderNum())
                            .orderAt(order.getOrderedAt())
                            .orderStatus(order.getStatus())
                            .items(items)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
