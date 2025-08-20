package org.example.tryonx.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.tryonx.product.domain.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class ProductDto {
    private Integer productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private Long likeCount;
    private BigDecimal discountRate;
    private BigDecimal discountPrice;

    public static ProductDto of(Product p, long likeCount) {
        String imageUrl = p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl();

        BigDecimal price = p.getPrice();
        BigDecimal discountRate = p.getDiscountRate();
        BigDecimal discountPrice = price;

        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = discountRate.divide(BigDecimal.valueOf(100)); // %를 소수로 변환
            discountPrice = price
                    .multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(0, RoundingMode.HALF_UP);
        }

        return new ProductDto(
                p.getProductId(),
                p.getProductName(),
                imageUrl,
                p.getPrice(),
                likeCount,
                discountRate,
                discountPrice
        );
    }

}

