package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.enums.DeliveryStatus;
import org.example.tryonx.enums.OrderStatus;

import java.math.BigDecimal;
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
    private DeliveryStatus deliveryStatus;
    private BigDecimal totalPrice;
}
