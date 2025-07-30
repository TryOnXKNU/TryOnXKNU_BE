package org.example.tryonx.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.enums.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemInfoDto {
    private Size size;
    private Integer stock;
    private ProductStatus status;
    private Double length;
    private Double shoulder;
    private Double chest;
    private Double sleeve_length;
    private Double waist;
    private Double thigh;
    private Double rise;
    private Double hem;
    private Double hip;
}
