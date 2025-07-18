package org.example.tryonx.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ExchangeResponseDto {
    private Integer exchangeId;
    private String status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
}
