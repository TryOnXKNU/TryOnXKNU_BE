package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.orders.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInfoDto {
    private Integer orderId;
    private OrderStatus orderStatus;
    private LocalDateTime orderAt;
    private String orderNum;
    private String name;
    private String phoneNumber;
    private String email;
    private String address;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private Integer usedPoints;
    private String deliveryFee;
    private BigDecimal finalAmount;
    private BigDecimal discountRate;
    private String paymentMethod;
    private List<OrderInfoItemDto> items;
}
