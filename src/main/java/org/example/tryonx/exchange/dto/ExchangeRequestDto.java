package org.example.tryonx.exchange.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeRequestDto {
    private Integer orderId;
    private Integer orderItemId;
    private String reason;
}
