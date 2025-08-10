package org.example.tryonx.orders.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentCompleteResDto {
    private String imp_uid;
    private String merchant_uid;
    private String status;      // paid 등
    private Integer amount;     // KRW 정수
    private LocalDateTime paidAt;
    private boolean verified;
}
