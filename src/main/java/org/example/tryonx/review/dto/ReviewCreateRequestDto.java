package org.example.tryonx.review.dto;

import lombok.Getter;

@Getter
public class ReviewCreateRequestDto {
    private Integer orderItemId;
    private String content;
    private int rating;
}
