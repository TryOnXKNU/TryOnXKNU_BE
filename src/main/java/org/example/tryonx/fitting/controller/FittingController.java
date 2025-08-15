package org.example.tryonx.fitting.controller;

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

    public FittingController(MemberService memberService, FittingService fittingService) {
        this.memberService = memberService;
        this.fittingService = fittingService;
    }

    @GetMapping
    public ResponseEntity<?> fittingPageInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        FittingResponse pageData = fittingService.getFittingPageData(email, null);
        return ResponseEntity.ok(pageData);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<?> fittingPageInfoByCategory(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Integer categoryId) {
        String email = userDetails.getUsername();
        FittingResponse pageData = fittingService.getFittingPageData(email, categoryId);
        return ResponseEntity.ok(pageData);
    }

    @PostMapping("/body-shape")
    public ResponseEntity<?> selectBodyShape(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BodyShapeRequest request) {
        String email = userDetails.getUsername();
        BodyShape response = fittingService.updateBodyShape(email, request);
        return ResponseEntity.ok(response);
    }
}
