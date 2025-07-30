package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.enums.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemDto {
    private Size size;
    private Integer stock;
    private ProductStatus status;
}
