package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Integer orderItemId;
    private Integer productId;
    private String productName;
    private Size size;
    private Integer quantity;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal discountRate;
    private BigDecimal discountPrice;
}
