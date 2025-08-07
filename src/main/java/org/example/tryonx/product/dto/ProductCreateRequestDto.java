package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private List<ProductItemInfoDto> productItemInfoDtos;
    private BigDecimal discountRate;
    private Integer categoryId;
    private ProductStatus status;
    private BodyShape bodyShape;
}
