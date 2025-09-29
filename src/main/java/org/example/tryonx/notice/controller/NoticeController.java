package org.example.tryonx.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.notice.dto.NoticeResponseDto;
import org.example.tryonx.notice.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notice")
@Tag(name = "Users Notices API", description = "회원 알림 API")
public class NoticeController {
    private final NoticeService noticeService;
    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @Operation(summary = "알림 조회")
    public ResponseEntity<List<NoticeResponseDto>> getMyNotices(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<NoticeResponseDto> myNotifications = noticeService.findByEmail(email);
        return ResponseEntity.ok(myNotifications);
    }
}
