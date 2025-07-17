package org.example.tryonx.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;
import org.example.tryonx.enums.Size;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewDto {
    private String memberNickname;
    private Integer height;
    private Integer weight;
    private String profileImageUrl;
    private Integer rating;

    private Integer productId;
    private String productName;
    private Size size;
    private String description;
    private LocalDateTime createdAt;
    private List<String> reviewImages;
}
