package org.example.tryonx.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Asks API", description = "관리자 문의 관리 API")
@RequiredArgsConstructor
public class AdminAskController {
    private final AdminAskService adminAskService;

    @GetMapping("/new")
    @Operation(summary = "새로운 문의 조회")
    public ResponseEntity<List<AskListDto>> getNewAsks() {
        return ResponseEntity.ok(adminAskService.getNewAsks());
    }

    @GetMapping("/completed")
    @Operation(summary = "답변 완료 문의 조회")
    public ResponseEntity<List<AskListDto>> getCompletedAsks() {
        return ResponseEntity.ok(adminAskService.getCompletedAsks());
    }

    @PostMapping("/answer")
    @Operation(summary = "문의 답변 등록")
    public ResponseEntity<Void> answerAsk(@RequestBody AskAnswerRequestDto dto) {
        adminAskService.answerAsk(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/completed/{askId}")
    @Operation(summary = "답변 완료된 문의 상세 조회")
    public ResponseEntity<CompletedAskDetailsDto> getCompletedAskDetail(@PathVariable Long askId) {
        CompletedAskDetailsDto dto = adminAskService.getCompletedAskDetail(askId);
        return ResponseEntity.ok(dto);
    }
}
