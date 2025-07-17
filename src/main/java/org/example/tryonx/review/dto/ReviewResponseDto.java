package org.example.tryonx.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
    private Long reviewId;
    private Integer productId;
    private String productName;
    private Size size;
    private String description;
    private Integer rating;
    private LocalDateTime createdAt;
    private String productImage;
    private List<String> reviewImages;
}
