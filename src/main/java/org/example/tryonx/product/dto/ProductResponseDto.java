package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.review.dto.ProductReviewDto;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private Long likeCount;
    private Integer categoryId;
    private String description;
    private List<String> productImages;
    private List<ProductItemInfoDto> productItems;
    private List<ProductReviewDto> productReviews;

    public ProductResponseDto(Integer productId, String productName, BigDecimal productPrice, Integer categoryId, String description, List<String> productImages, List<ProductItemInfoDto> productItems) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.categoryId = categoryId;
        this.description = description;
        this.productImages = productImages;
        this.productItems = productItems;
    }
}
