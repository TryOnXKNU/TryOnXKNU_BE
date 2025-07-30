package org.example.tryonx.admin.dto;

import lombok.Getter;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.product.dto.ProductItemInfoDto;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class ProductDetailUpdateDto {
    private List<ProductItemInfoDto> productItemInfoDtos;
    private BigDecimal price;
    private BigDecimal discountRate;
    private Integer categoryId;
    private BodyShape bodyShape;
}
