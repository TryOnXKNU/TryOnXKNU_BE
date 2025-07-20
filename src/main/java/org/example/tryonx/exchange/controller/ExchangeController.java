package org.example.tryonx.exchange.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.exchange.dto.ExchangeRequestDto;
import org.example.tryonx.exchange.dto.ExchangeResponseDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;

    //교환 신청
    @PostMapping
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
    public ResponseEntity<List<ExchangeResponseDto>> getMyExchanges(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExchangeResponseDto> list = exchangeService.getMyExchanges(email);
        return ResponseEntity.ok(list);
    }

    // 교환 신청 상세 조회
    @GetMapping("/{exchangeId}")
    public ResponseEntity<ExchangeResponseDto> getExchangeDetail(@PathVariable Integer exchangeId,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ExchangeResponseDto dto = exchangeService.getExchangeDetail(email, exchangeId);
        return ResponseEntity.ok(dto);
    }

}
