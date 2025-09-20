package org.example.tryonx.returns.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnDetailDto {
    private Integer returnId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private String orderNum;
    private BigDecimal price;
    private Integer quantity;
    private Size size;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private String status;
    private String productName;
    private String productImageUrl;
    private String rejectReason;
    private BigDecimal discountRate;
    private BigDecimal discountPrice;
}
