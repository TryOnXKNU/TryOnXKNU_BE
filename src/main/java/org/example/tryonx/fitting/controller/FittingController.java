package org.example.tryonx.fitting.controller;

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
    public ResponseEntity<?> fittingPageInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        FittingResponse pageData = fittingService.getFittingPageData(email);
        return ResponseEntity.ok(pageData);
    }


    @PostMapping("/body-shape")
    public ResponseEntity<?> selectBodyShape(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BodyShapeRequest request) {
        String email = userDetails.getUsername();
        BodyShape response = fittingService.updateBodyShape(email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/try-on/single")
    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId) throws Exception {
        String email = userDetails.getUsername();
        String filename = comfyUiService.executeFittingFlow(email, productId);
        return ResponseEntity.ok("/upload/fitting/downloaded_"+ filename);
    }

//    @PostMapping("/try-on/dual")
//    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId1, @RequestParam Integer productId2) throws Exception {
//        String email = userDetails.getUsername();
//        String filename = comfyUiService.executeFittingTwoClothesFlow(email, productId1, productId2);
//        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
//    }
}
