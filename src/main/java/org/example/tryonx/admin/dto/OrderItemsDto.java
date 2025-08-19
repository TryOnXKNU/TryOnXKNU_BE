package org.example.tryonx.admin.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemsDto {
    private Integer orderItemId;
    private String productName;
    private Integer quantity;
}
