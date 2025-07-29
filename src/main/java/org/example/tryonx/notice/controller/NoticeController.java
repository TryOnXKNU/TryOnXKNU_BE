package org.example.tryonx.notice.controller;

import org.example.tryonx.notice.domain.Notification;
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
public class NoticeController {
    private final NoticeService noticeService;
    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ResponseEntity<List<NoticeResponseDto>> getMyNotices(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<NoticeResponseDto> myNotifications = noticeService.findByEmail(email);
        return ResponseEntity.ok(myNotifications);
    }
}
