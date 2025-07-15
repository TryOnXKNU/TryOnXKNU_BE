package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Integer orderItemId;
    private String productName;
    private Size size;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
}
