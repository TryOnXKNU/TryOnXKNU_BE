package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberOrderHistory {
    private String profileUrl;
    private String name;
    private Long memberId;
    private Integer orderId;
    private LocalDateTime orderedAt;
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private OrderStatus orderStatus;
}
