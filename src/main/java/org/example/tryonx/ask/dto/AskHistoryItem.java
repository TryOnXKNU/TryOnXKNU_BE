package org.example.tryonx.ask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.AnswerStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskHistoryItem {
    private Long askId;
    private String title;
    private List<String> imageUrls;
    private AnswerStatus answerStatus;
    private LocalDateTime createdAt;
}
