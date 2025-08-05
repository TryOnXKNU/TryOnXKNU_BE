package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskAnswerRequestDto {
    private Long askId;
    private Integer orderItemId;
    private String title;
    private String productName;
    private String content;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private String answer;
}
