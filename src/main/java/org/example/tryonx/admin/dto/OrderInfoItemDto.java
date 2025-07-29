package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInfoItemDto {
    private Integer orderItemId;
    private String productName;
    private String imageUrl;
    private Size size;
    private Integer quantity;
    private BigDecimal price;
}
