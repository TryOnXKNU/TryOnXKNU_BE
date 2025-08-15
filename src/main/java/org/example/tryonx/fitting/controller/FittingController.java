package org.example.tryonx.fitting.controller;

import org.example.tryonx.fitting.dto.BodyShapeRequest;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fitting")
public class FittingController {
    private final MemberService memberService;
    public FittingController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/body-shape")
    public ResponseEntity<?> selectBodyShape(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BodyShapeRequest request) {
        String email = userDetails.getUsername();
        String response = memberService.updateBodyShape(email, request);
        return ResponseEntity.ok(response);
    }
}
