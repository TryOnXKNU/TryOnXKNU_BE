package org.example.tryonx.exchange.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.tryonx.enums.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExchangeDetailDto {
    private Integer exchangeId;
    private Long memberId;
    private Integer orderId;
    private Integer orderItemId;
    private String orderNum;
    private BigDecimal price;
    private Size size;
    private Integer quantity;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private String status;
    private String productName;
    private String productImageUrl;
    private String rejectReason;
    private BigDecimal discountRate;
    private BigDecimal discountPrice;
    private Size exchangeSize;
}
