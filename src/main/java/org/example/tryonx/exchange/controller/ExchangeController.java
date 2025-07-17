package org.example.tryonx.exchange.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.exchange.dto.ExchangeRequestDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exchanges")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;

    @PostMapping
    public ResponseEntity<String> requestExchange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ExchangeRequestDto dto
    ) {
        String email = userDetails.getUsername();
        exchangeService.requestExchange(email, dto);
        return ResponseEntity.ok("교환 신청 완료");
    }

}
