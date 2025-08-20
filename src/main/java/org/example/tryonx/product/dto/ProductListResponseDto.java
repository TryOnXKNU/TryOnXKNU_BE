package org.example.tryonx.product.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListResponseDto {
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private Long likeCount;
    private Integer categoryId;
    private String thumbnailUrl;
    private BigDecimal discountRate;
    private BigDecimal discountPrice;
    private Double averageRating;
    private Integer reviewCount;
}
