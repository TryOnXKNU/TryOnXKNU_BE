package org.example.tryonx.ask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.AnswerStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskHistoryItem {
    private String title;
    private AnswerStatus answerStatus;
    private LocalDateTime createdAt;
}
