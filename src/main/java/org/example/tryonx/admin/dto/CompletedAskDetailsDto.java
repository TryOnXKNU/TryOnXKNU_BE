package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedAskDetailsDto {
    private Long askId;
    private String nickname;
    private String productName;
    private Size size;
    private String askTitle;
    private String content;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
    private List<String> userImageUrls;
    private List<String> productImageUrls;
}
