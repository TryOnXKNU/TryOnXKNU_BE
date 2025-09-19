package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.tryonx.enums.DeliveryStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DeliveryHistoryDto {
    private String orderNum;
    private LocalDateTime changedAt;
    private DeliveryStatus deliveryStatus;
}
