package org.example.tryonx.admin.ask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskAnswerRequestDto {
    private Long askId;
    private String answer;
}
