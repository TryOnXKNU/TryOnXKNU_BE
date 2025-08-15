package org.example.tryonx.fitting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FittingProductInfo {
    private Integer productId;
    private Integer categoryId;
    private String productName;
    private String imgUrl;
    private Boolean bestFit;
}
