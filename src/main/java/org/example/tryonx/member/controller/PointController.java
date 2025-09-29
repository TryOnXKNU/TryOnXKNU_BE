package org.example.tryonx.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.dto.PointHistoryDto;
import org.example.tryonx.member.service.PointService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
@Tag(name = "Users Points API", description = "회원 적립금 API")
public class PointController {
    private final PointService pointService;

    @GetMapping("/history")
    @Operation(summary = "적립금 내역 조회")
    public List<PointHistoryDto> getPointHistory(@AuthenticationPrincipal UserDetails user) {
        String email = user.getUsername();
        return pointService.getPointHistory(email);
    }
}
