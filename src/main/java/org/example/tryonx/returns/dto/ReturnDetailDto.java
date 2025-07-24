package org.example.tryonx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReturnDetailDto {
    private Integer returnId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private BigDecimal price;
    private Integer quantity;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String status;
    private String productName;
    private String productImageUrl;
}
