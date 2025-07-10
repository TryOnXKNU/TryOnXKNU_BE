package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponseDto {
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private Long likeCount;
    private Integer categoryId;
    private String thumbnailUrl;
}
