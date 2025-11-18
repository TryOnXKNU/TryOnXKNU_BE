package org.example.tryonx.fitting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomClothesInfo {
    private String clothesId;
    private Integer categoryId;
    private String clothesName;
    private String imgUrl;
}
