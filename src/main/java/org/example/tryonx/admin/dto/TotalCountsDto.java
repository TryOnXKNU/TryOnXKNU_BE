package org.example.tryonx.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalCountsDto {
    private long exchangeTotalCount;
    private long returnTotalCount;
    private long askTotalCount;
}
