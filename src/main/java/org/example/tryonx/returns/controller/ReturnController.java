package org.example.tryonx.returns.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.returns.dto.ReturnRequestDto;
import org.example.tryonx.returns.dto.ReturnResponseDto;
import org.example.tryonx.returns.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/return")
@RequiredArgsConstructor
public class ReturnController {
    private final ReturnService returnService;

    //반품 신청
    @PostMapping
    public ResponseEntity<String> requestReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReturnRequestDto dto
    ) {
        String email = userDetails.getUsername();
        returnService.requestReturn(email, dto);
        return ResponseEntity.ok("반품 신청 완료");
    }

    // 반품 신청 내역 조회
    @GetMapping("/my")
    public ResponseEntity<List<ReturnResponseDto>> getMyReturns(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ReturnResponseDto> list = returnService.getMyReturns(email);
        return ResponseEntity.ok(list);
    }

    // 반품 신청 상세 조회
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponseDto> getReturnDetail(@PathVariable Integer returnId,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReturnResponseDto dto = returnService.getReturnDetail(email, returnId);
        return ResponseEntity.ok(dto);
    }
}
