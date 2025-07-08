package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.category.Category;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.enums.ProductStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequestDto {
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer sizeXsStock;
    private Integer sizeSStock;
    private Integer sizeMStock;
    private Integer sizeLStock;
    private Integer sizeXLStock;
    private Integer sizeFreeStock;
    private BigDecimal discountRate;
    private Integer categoryId;
    private ProductStatus status;
    private BodyShape bodyShape;
}
