package org.example.tryonx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReturnResponseDto {
    private Integer returnId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private BigDecimal price;
    private Integer quantity;
    private String reason;
    private String status;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime returnApprovedAt;
    private String productName;
    private String productImageUrl;
}
