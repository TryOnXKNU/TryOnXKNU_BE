package org.example.tryonx.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalCountsDto {
    private long exchangeTotalCount;
    private long returnTotalCount;
    private long askTotalCount;
    private long newMemberCount;
    private long totalMemberCount;
    private long orderTotalCount;
    private BigDecimal todaySalesAmount;
}
