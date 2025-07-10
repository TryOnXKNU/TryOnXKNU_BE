package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItem {
    private String orderId;
    private String productName;
    private Size size;
    private Integer quantity;
    private BigDecimal price;
    private String imgUrl;
    private Integer orderItemsCount;
}
