package org.example.tryonx.admin.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductStockAndStateUpdateDto {
    private List<ProductStatusDto> item;
}
