package org.example.tryonx.orders.payment.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;
import org.example.tryonx.orders.order.dto.OrderPreviewRequestDto;

import java.util.List;

@Getter
public class PrecheckRequstDto {
    private List<Item> items;

    @Getter
    public static class Item {
        private Integer productId;
        private Size size;
        private Integer quantity;
    }
}
