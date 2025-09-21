package org.example.tryonx.exchange.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.tryonx.enums.Size;

@Getter
@Setter
public class ExchangeRequestDto {
    private Integer orderId;
    private Integer orderItemId;
    private String reason;
    private Size exchangeSize;
}
