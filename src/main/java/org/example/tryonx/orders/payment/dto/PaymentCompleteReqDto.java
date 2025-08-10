package org.example.tryonx.orders.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCompleteReqDto {
    private String imp_uid;
    private String merchant_uid;
}
