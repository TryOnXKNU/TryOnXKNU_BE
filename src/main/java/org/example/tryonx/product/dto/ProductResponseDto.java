package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
