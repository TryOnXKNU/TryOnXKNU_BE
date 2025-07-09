package org.example.tryonx.orders.order.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;

import java.util.List;

@Getter
public class OrderPreviewRequestDto {
    private List<Item> items;

    @Getter
    public static class Item {
        private Integer productId;
        private Size size;
        private Integer quantity;
    }
}

