package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.DeliveryStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDto {
    private Integer orderId;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime updatedAt;
}
