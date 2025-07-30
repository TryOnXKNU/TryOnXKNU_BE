package org.example.tryonx.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductListDto {
    private Integer productId;
    private String productName;
    private String productCode;
    private BigDecimal productPrice;
    private BigDecimal discountRate;
    private String imageUrl;
    private List<ProductItemDto> productItems;
}
