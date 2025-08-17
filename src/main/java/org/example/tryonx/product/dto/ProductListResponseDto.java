package org.example.tryonx.product.dto;

import lombok.*;

import java.math.BigDecimal;

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

    private BigDecimal discountRate;
    private Double averageRating;
    private Integer reviewCount;
}
