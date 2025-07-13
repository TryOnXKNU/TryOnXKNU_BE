package org.example.tryonx.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.AskAnswerRequestDto;
import org.example.tryonx.admin.dto.AskListDto;
import org.example.tryonx.admin.dto.CompletedAskDetailsDto;
import org.example.tryonx.admin.service.AdminAskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/asks")
@RequiredArgsConstructor
public class AdminAskController {
    private final AdminAskService adminAskService;

    @GetMapping("/new")
    public ResponseEntity<List<AskListDto>> getNewAsks() {
        return ResponseEntity.ok(adminAskService.getNewAsks());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<AskListDto>> getCompletedAsks() {
        return ResponseEntity.ok(adminAskService.getCompletedAsks());
    }

    @PostMapping("/answer")
    public ResponseEntity<Void> answerAsk(@RequestBody AskAnswerRequestDto dto) {
        adminAskService.answerAsk(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/completed/{askId}")
    public ResponseEntity<CompletedAskDetailsDto> getCompletedAskDetail(@PathVariable Long askId) {
        CompletedAskDetailsDto dto = adminAskService.getCompletedAskDetail(askId);
        return ResponseEntity.ok(dto);
    }
}
