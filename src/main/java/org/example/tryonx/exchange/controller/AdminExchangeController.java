package org.example.tryonx.exchange.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.exchange.dto.ExchangeDetailDto;
import org.example.tryonx.exchange.dto.ExchangeListDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/exchanges")
public class AdminExchangeController {
    private final ExchangeService exchangeService;

    //전체 교환 목록 조회
    @GetMapping("/all")
    public ResponseEntity<List<ExchangeListDto>> getAllExchanges() {
        List<ExchangeListDto> exchangeList = exchangeService.getExchangeList();
        return ResponseEntity.ok(exchangeList);
    }

    //교환 상세 정보
    @GetMapping("/{exchangeId}")
    public ResponseEntity<ExchangeDetailDto> getExchangeDetail(@PathVariable Integer exchangeId) {
        ExchangeDetailDto dto = exchangeService.findByExchangeId(exchangeId);
        return ResponseEntity.ok(dto);
    }

}
