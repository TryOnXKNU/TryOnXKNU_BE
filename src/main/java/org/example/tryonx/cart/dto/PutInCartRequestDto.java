package org.example.tryonx.cart.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;

import java.util.List;

@Getter
public class PutInCartRequestDto {
    private List<ReqItem> items;

    @Getter
    public static class ReqItem {
        private Integer productId;
        private Size size;
        private Integer quantity;
    }
}
