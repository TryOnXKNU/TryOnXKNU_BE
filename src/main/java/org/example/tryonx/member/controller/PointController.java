package org.example.tryonx.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.dto.PointHistoryDto;
import org.example.tryonx.member.service.PointService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {
    private final PointService pointService;

    @GetMapping("/history")
    public List<PointHistoryDto> getPointHistory(@AuthenticationPrincipal UserDetails user) {
        String email = user.getUsername();
        return pointService.getPointHistory(email);
    }
}
