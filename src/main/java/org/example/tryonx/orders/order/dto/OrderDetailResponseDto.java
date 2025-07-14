package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;
import org.example.tryonx.orders.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {
    private Integer orderId;
    private String orderNum;
    private MemberInfoDto memberInfo;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private OrderStatus status;
    private Integer usedPoints;
    private List<Item> items;
    private Integer orderItemsCount;
    private LocalDateTime orderedAt;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private Size size;
        private String discountRate;
        private String imgUrl;
    }
}
