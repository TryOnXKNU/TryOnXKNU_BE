package org.example.tryonx.exchange.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.exchange.dto.ExchangeDetailDto;
import org.example.tryonx.exchange.dto.ExchangeListDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/exchanges")
@Tag(name = "Admin Exchanges API", description = "관리자 교환 관리 API")
public class AdminExchangeController {
    private final ExchangeService exchangeService;

    //전체 교환 목록 조회
    @GetMapping("/all")
    @Operation(summary = "전체 교환 목록 조회")
    public ResponseEntity<List<ExchangeListDto>> getAllExchanges() {
        List<ExchangeListDto> exchangeList = exchangeService.getExchangeList();
        return ResponseEntity.ok(exchangeList);
    }

    //교환 상세 정보
    @GetMapping("/{exchangeId}")
    @Operation(summary = "교환 상세 조회")
    public ResponseEntity<ExchangeDetailDto> getExchangeDetailForAdmin(@PathVariable Integer exchangeId) {
        ExchangeDetailDto dto = exchangeService.findByExchangeIdForAdmin(exchangeId);
        return ResponseEntity.ok(dto);
    }

    //교환 상태 변경
    @PatchMapping("/{exchangeId}/status/{status}")
    @Operation(summary = "교환 상태 변경")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Integer exchangeId,
            @PathVariable ExchangeStatus status,
            @RequestParam(required = false) String reason
    ) {
        exchangeService.updateExchangeStatus(exchangeId, status, reason);
        return ResponseEntity.ok().build();
    }

    //교환 상태별 조회
//    @GetMapping("/status")
//    public ResponseEntity<List<ExchangeListDto>> getExchangesByStatus(
//            @RequestParam("status") ExchangeStatus status
//    ) {
//        List<ExchangeListDto> result = exchangeService.getExchangesByStatus(status);
//        return ResponseEntity.ok(result);
//    }

}
