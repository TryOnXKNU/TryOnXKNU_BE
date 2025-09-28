package org.example.tryonx.exchange.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.Size;
import org.example.tryonx.exchange.dto.ExchangeDetailDto;
import org.example.tryonx.exchange.dto.ExchangeRequestDto;
import org.example.tryonx.exchange.dto.ExchangeResponseDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange")
@Tag(name = "User Exchanges API", description = "사용자 교환 API")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;

    // 교환 가능한 사이즈 조회
    @GetMapping("/{orderItemId}/available-sizes")
    @Operation(summary = "교환 가능한 사이즈 조회")
    public ResponseEntity<List<Size>> getAvailableSizes(@PathVariable Integer orderItemId) {
        return ResponseEntity.ok(exchangeService.getAvailableSizes(orderItemId));
    }

    //교환 신청
    @PostMapping
    @Operation(summary = "교환 신청")
    public ResponseEntity<String> requestExchange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ExchangeRequestDto dto
    ) {
        String email = userDetails.getUsername();
        exchangeService.requestExchange(email, dto);
        return ResponseEntity.ok("교환 신청 완료");
    }

    // 교환 신청 내역 조회
    @GetMapping("/my")
    @Operation(summary = "교환 내역 조회")
    public ResponseEntity<List<ExchangeResponseDto>> getMyExchanges(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExchangeResponseDto> list = exchangeService.getMyExchanges(email);
        return ResponseEntity.ok(list);
    }

    // 교환 신청 상세 조회
    @GetMapping("/{exchangeId}")
    @Operation(summary = "교환 상세 조회")
    public ResponseEntity<ExchangeDetailDto> getExchangeDetail(
            @PathVariable Integer exchangeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        String email = userDetails.getUsername();
        ExchangeDetailDto dto = exchangeService.findByExchangeId(email, exchangeId);
        return ResponseEntity.ok(dto);
    }

}
