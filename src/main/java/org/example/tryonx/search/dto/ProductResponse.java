package org.example.tryonx.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.tryonx.image.domain.ProductImage;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String productName;
    private BigDecimal price;
    private BigDecimal discountRate;
    private List<ProductImageResponse> images;
}
