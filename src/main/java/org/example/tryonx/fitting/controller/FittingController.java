package org.example.tryonx.fitting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.comfy.service.ComfyUiService;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.dto.BodyShapeRequest;
import org.example.tryonx.fitting.dto.FittingResponse;
import org.example.tryonx.fitting.service.FittingService;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fitting")
@Tag(name = "User AI Fitting API", description = "회원 AI 피팅 API")
public class FittingController {
    private final MemberService memberService;
    private final FittingService fittingService;
    private final ComfyUiService comfyUiService;

    public FittingController(MemberService memberService, FittingService fittingService, ComfyUiService comfyUiService) {
        this.memberService = memberService;
        this.fittingService = fittingService;
        this.comfyUiService = comfyUiService;
    }

    @GetMapping
    @Operation(summary = "체형 조회")
    public ResponseEntity<?> fittingPageInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        FittingResponse pageData = fittingService.getFittingPageData(email);
        return ResponseEntity.ok(pageData);
    }


    @PostMapping("/body-shape")
    @Operation(summary = "체형 변경")
    public ResponseEntity<?> selectBodyShape(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BodyShapeRequest request) {
        String email = userDetails.getUsername();
        BodyShape response = fittingService.updateBodyShape(email, request);
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/try-on/single")
//    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId) throws Exception {
//        String email = userDetails.getUsername();
//        String filename = comfyUiService.executeFittingFlow(email, productId);
//        return ResponseEntity.ok("/upload/fitting/downloaded_"+ filename);
//    }

    @PostMapping("/try-on/dual")
    @Operation(summary = "AI 피팅")
    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId1, @RequestParam(required = false) Integer productId2) throws Exception {
        String email = userDetails.getUsername();
        String filename = comfyUiService.executeFittingTwoClothesFlow(email, productId1, productId2);
        return ResponseEntity.ok("https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename);
    }
}
