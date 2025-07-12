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

    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getProductId(),
                product.getProductName(),
                product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl(),
                product.getPrice()
        );
    }
}

