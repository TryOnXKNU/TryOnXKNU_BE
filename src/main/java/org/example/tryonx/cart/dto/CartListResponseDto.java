package org.example.tryonx.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartListResponseDto {
    private BigDecimal productPrice;
    private BigDecimal deliveryFee;
    private BigDecimal totalPrice;
    private Integer expectedPoint;
    private List<Item> items;
}
