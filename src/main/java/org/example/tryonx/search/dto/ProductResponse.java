package org.example.tryonx.search.dto;

import lombok.*;
import org.example.tryonx.image.domain.ProductImage;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal discountRate;
    private List<ProductImageResponse> images;
    private BigDecimal discountPrice;
    private Long likeCount;
    private Double averageRating;
}
