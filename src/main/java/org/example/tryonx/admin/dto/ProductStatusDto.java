package org.example.tryonx.admin.dto;

import lombok.Getter;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.enums.Size;

@Getter
public class ProductStatusDto {
    private Size size;
    private ProductStatus status;
}
