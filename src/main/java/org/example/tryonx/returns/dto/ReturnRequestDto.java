package org.example.tryonx.returns.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRequestDto {
    private Integer orderId;
    private Integer orderItemId;
    private String reason;
}
