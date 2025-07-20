package org.example.tryonx.orders.order.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;

import java.util.List;

@Getter
public class OrderRequestDto {
    private List<Item> items;
    private Integer point;

    @Getter
    public static class Item {
        private Integer productId;
        private Size size;
        private Integer quantity;
        private Long cartItemId;
    }
}
