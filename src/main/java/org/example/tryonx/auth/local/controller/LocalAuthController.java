package org.example.tryonx.auth.local.controller;

import org.example.tryonx.auth.local.dto.LoginRequestDto;
import org.example.tryonx.auth.local.dto.SignupRequestDto;
import org.example.tryonx.auth.local.service.LocalAuthService;
import org.example.tryonx.auth.local.token.JwtTokenProvider;
import org.example.tryonx.member.domain.Member;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class LocalAuthController {
    private final LocalAuthService localAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    public LocalAuthController(LocalAuthService localAuthService, JwtTokenProvider jwtTokenProvider) {
        this.localAuthService = localAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDto signupRequestDto) {
        Member member = localAuthService.create(signupRequestDto);
        return ResponseEntity.ok().body(member);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        Member member = localAuthService.login(loginRequestDto);
        String jwtToken = jwtTokenProvider.createtoken(member.getEmail(),member.getRole().toString());
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("email", member.getEmail());
        loginInfo.put("role", member.getRole());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }
}
