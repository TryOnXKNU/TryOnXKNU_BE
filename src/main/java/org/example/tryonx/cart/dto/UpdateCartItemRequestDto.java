package org.example.tryonx.cart.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;

@Getter
public class UpdateCartItemRequestDto {
    private Long cartItemId;
    private Integer productId;
    private int quantity;
    private Size size;
}
