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
    private BigDecimal monthSalesAmount;
    private BigDecimal totalSalesAmount;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DailySalesDto {
        private LocalDate date;
        private BigDecimal amount;        // 일 매출
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlySalesDto {
        private YearMonth month;
        private BigDecimal amount;        // 월 매출
    }
}
