package org.example.tryonx.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.tryonx.member.domain.PointHistory;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PointHistoryDto {
    private int amount;
    private String description;
    private LocalDateTime createdAt;

    public static PointHistoryDto from(PointHistory history) {
        return new PointHistoryDto(
                history.getAmount(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}
