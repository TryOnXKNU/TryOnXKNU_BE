package org.example.tryonx.auth.local.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.auth.email.service.EmailService;
import org.example.tryonx.auth.local.dto.*;
import org.example.tryonx.auth.local.service.LocalAuthService;
import org.example.tryonx.auth.local.token.JwtTokenProvider;
import org.example.tryonx.member.domain.Member;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API", description = "회원가입/로그인 API")
public class LocalAuthController {
    private final LocalAuthService localAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public LocalAuthController(LocalAuthService localAuthService, JwtTokenProvider jwtTokenProvider, EmailService emailService) {
        this.localAuthService = localAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDto signupRequestDto) {
        Member member = localAuthService.create(signupRequestDto);
        return ResponseEntity.ok().body(member);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        Member member = localAuthService.login(loginRequestDto);
        String jwtToken = jwtTokenProvider.createtoken(member.getEmail(),member.getRole().toString());
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("email", member.getEmail());
        loginInfo.put("role", member.getRole());
        loginInfo.put("name", member.getName());
        loginInfo.put("nickname", member.getNickname());
        loginInfo.put("height", member.getHeight());
        loginInfo.put("weight", member.getWeight());
        loginInfo.put("token", jwtToken);
        return new ResponseEntity<>(loginInfo, HttpStatus.OK);
    }
    @GetMapping("/duplicate-nickname")
    @Operation(summary = "닉네임 중복 확인")
    public ResponseEntity<?> checkNickName(@RequestParam String nickname) {
        boolean available = localAuthService.isNicknameExist(nickname);
        return ResponseEntity.ok().body(available);
    }

    @PostMapping("/find-id")
    @Operation(summary = "아이디 찾기")
    public ResponseEntity<?> findUserId(@RequestBody FindUserIdRequest findUserIdRequest) {
        String userId = localAuthService.findIdByPhoneNumber(findUserIdRequest.phoneNumber());
        return ResponseEntity.ok().body(userId);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "비밀번호 재설정")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        localAuthService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok("비밀번호 재설정이 완료되었습니다.");
    }

    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복 확인")
    public ResponseEntity<String> checkEmail(@RequestBody CheckEmailRequest checkEmailRequest) {
        boolean b = localAuthService.IsExistedEmail(checkEmailRequest.email());
        if(b)
            return ResponseEntity.ok("중복된 이메일이 존재합니다.");
        return ResponseEntity.ok("중복된 이메일이 존재하지 않습니다.");
    }
}
