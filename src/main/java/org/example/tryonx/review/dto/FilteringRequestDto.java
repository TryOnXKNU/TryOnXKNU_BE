package org.example.tryonx.review.dto;

import lombok.Getter;
import org.example.tryonx.enums.Size;

@Getter
public class FilteringRequestDto {
    private Integer productId;
    private Size size;
    private Integer minHeight;
    private Integer maxHeight;
    private Integer minWeight;
    private Integer maxWeight;
}
