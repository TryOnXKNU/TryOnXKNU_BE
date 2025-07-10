package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderPreviewResponseDto {
    private MemberInfoDto memberInfo;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Integer expectedPoint;
    private List<Item> items;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private Size size;
        private String discountRate;
    }
}
