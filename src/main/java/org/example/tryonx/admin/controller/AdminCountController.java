package org.example.tryonx.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.TotalCountsDto;
import org.example.tryonx.exchange.service.ExchangeService;
import org.example.tryonx.returns.service.ReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCountController {
    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private ReturnService returnService;
    
    @GetMapping("/admin/total-counts")
    public ResponseEntity<TotalCountsDto> getTotalCounts() {
        long exchangeCount = exchangeService.countAllExchanges();
        long returnCount = returnService.countAllReturns();

        TotalCountsDto dto = new TotalCountsDto(exchangeCount, returnCount);
        return ResponseEntity.ok(dto);
    }

}
