package org.example.tryonx.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.SalesDto;
import org.example.tryonx.admin.dto.TotalCountsDto;
import org.example.tryonx.admin.service.MemberListService;
import org.example.tryonx.ask.service.AskService;
import org.example.tryonx.exchange.service.ExchangeService;
import org.example.tryonx.orders.order.service.OrderService;
import org.example.tryonx.returns.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Counts API", description = "관리자 메인 홈 API")
public class AdminCountController {
    private final ExchangeService exchangeService;

    private final ReturnService returnService;

    private final AskService askService;

    private final MemberListService memberListService;

    private final OrderService orderService;
    
    @GetMapping("/total-counts")
    @Operation(summary = "신규 회원 / 전체 회원 / 주문 접수 / 교환 접수 / 반품 접수 / 문의 접수")
    public ResponseEntity<TotalCountsDto> getTotalCounts() {
        long exchangeCount = exchangeService.countAllExchanges();
        long returnCount = returnService.countAllReturns();
        long askCount = askService.countAllAsks();
        long newMemberCount = memberListService.countNewMembers();
        long totalMemberCount = memberListService.countTotalMembers();
        long orderCount = orderService.countAllOrders();
        BigDecimal todaySalesAmount = orderService.getTodaySalesAmount();


        TotalCountsDto dto = new TotalCountsDto(exchangeCount, returnCount, askCount, newMemberCount, totalMemberCount, orderCount, todaySalesAmount);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sales-counts")
    @Operation(summary = "매출 조회 (오늘 매출액 / 월별 매출액 / 1년 매출액 / 총 매출액)")
    public ResponseEntity<SalesDto> getSalesCounts() {
        BigDecimal todaySalesAmount = orderService.getTodaySalesAmount();
        BigDecimal monthSalesAmount  = orderService.getMonthlySalesAmount(java.time.YearMonth.now());
        BigDecimal yearSalesAmount   = orderService.getYearlySalesAmount(java.time.Year.now().getValue());
        BigDecimal totalSalesAmount  = orderService.getTotalSalesAmount();

        SalesDto dto = new SalesDto(
                todaySalesAmount,
                monthSalesAmount,
                yearSalesAmount,
                totalSalesAmount
        );

        return ResponseEntity.ok(dto);
    }
}
