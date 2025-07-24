package org.example.tryonx.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeListDto {
    private Integer exchangeId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private LocalDateTime requestedAt;
    private String status;
    private BigDecimal price;
    private Integer quantity;
    private String productName;
    private String productImageUrl;
}
