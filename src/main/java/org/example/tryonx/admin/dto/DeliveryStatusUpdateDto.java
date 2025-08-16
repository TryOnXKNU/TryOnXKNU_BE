package org.example.tryonx.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.tryonx.enums.DeliveryStatus;

@Getter
@Setter
public class DeliveryStatusUpdateDto {
    private DeliveryStatus status;
}
