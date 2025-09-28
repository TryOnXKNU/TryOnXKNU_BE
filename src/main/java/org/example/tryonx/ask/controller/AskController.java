package org.example.tryonx.ask.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.ask.dto.AskHistoryItem;
import org.example.tryonx.ask.dto.AskListItem;
import org.example.tryonx.ask.dto.AskRequestDto;
import org.example.tryonx.ask.dto.AskResponseDto;
import org.example.tryonx.ask.service.AskService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ask")
@Tag(name = "User Asks API", description = "사용자 문의 API")
@RequiredArgsConstructor
public class AskController {
    private final AskService askService;

    //문의하기 조회
    @GetMapping("/available")
    @Operation(summary = "문의 가능한 목록 조회")
    public ResponseEntity<List<AskListItem>> getAskableItems(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<AskListItem> askableItems = askService.getAskableItems(email);
        return ResponseEntity.ok(askableItems);
    }

    //문의 작성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "문의 작성")
    public ResponseEntity<Void> createAsk(
            @RequestPart("dto") AskRequestDto dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        askService.createAsk(email, dto, images);
        return ResponseEntity.ok().build();
    }


    //문의내역 조회
    @GetMapping("/my")
    @Operation(summary = "문의내역 조회")
    public ResponseEntity<List<AskHistoryItem>> getMyAsks(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<AskHistoryItem> askList = askService.getMyAsks(email);
        return ResponseEntity.ok(askList);
    }

    //문의내역 상세보기
    @GetMapping("/{askId}")
    @Operation(summary = "문의내역 상세조회")
    public ResponseEntity<AskResponseDto> getAskDetail(@PathVariable Long askId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        AskResponseDto response = askService.getAskDetail(email, askId);
        return ResponseEntity.ok(response);
    }

    //문의 삭제
    @DeleteMapping("/{askId}")
    @Operation(summary = "문의 삭제")
    public ResponseEntity<Void> deleteAsk(@PathVariable Long askId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        askService.deleteAsk(email, askId);
        return ResponseEntity.ok().build();
    }


}
