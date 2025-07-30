package org.example.tryonx.ask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.AnswerStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponseDto {
    private Long askId;
    private Integer orderItemId;
    private String title;
    private String productName;
    private String size;
    private String content;
    private List<String> imageUrls;

    private LocalDateTime createdAt;

    private AnswerStatus answerStatus;
    private String answer;
    private LocalDateTime answeredAt;
}
