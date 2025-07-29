package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.orders.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListDto {
    private Integer orderId;
    private String orderNum;
    private LocalDateTime orderAt;
    private OrderStatus orderStatus;
    private List<OrderItemsDto> items;
}
