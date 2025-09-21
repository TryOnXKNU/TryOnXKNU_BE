package org.example.tryonx.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeResponseDto {
    private Integer exchangeId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private String status;
    private BigDecimal price;
    private Integer quantity;
    private Size size;
    private String productName;
    private String productImageUrl;
    private Size exchangeSize;
}
