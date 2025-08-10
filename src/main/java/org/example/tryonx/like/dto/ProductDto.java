package org.example.tryonx.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.tryonx.product.domain.Product;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductDto {
    private Integer productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private Long likeCount;

    public static ProductDto of(Product p, long likeCount) {
        String imageUrl = p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl();
        return new ProductDto(
                p.getProductId(),
                p.getProductName(),
                imageUrl,
                p.getPrice(),
                likeCount
        );
    }

}

