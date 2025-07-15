package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItem {
    private Integer orderId;
    private Long memberId;
    private String orderNum;
    private List<OrderItemDto> orderItem;
    private BigDecimal totalPrice;
    private Integer orderItemsCount;
    private LocalDateTime orderedAt;
}
