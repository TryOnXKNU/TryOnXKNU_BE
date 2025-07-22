package org.example.tryonx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReturnListDto {
    private Integer returnId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private LocalDateTime requestedAt;
    private String status;
    private BigDecimal price;
    private Integer quantity;
}
