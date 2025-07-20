package org.example.tryonx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReturnResponseDto {
    private Integer returnId;
    private String status;
    private String reason;
    private LocalDateTime returnRequestedAt;
    private LocalDateTime returnApprovedAt;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
}
