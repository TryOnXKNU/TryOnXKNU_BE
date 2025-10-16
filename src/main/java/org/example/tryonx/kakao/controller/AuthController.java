package org.example.tryonx.kakao.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tryonx.kakao.dto.KakaoLoginRequest;
import org.example.tryonx.kakao.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Login API", description = "카카오로그인 API")
public class AuthController {
    private final AuthService authService;

    @Value("${kakao.client.id}")
    private String clientKey;
    @Value("${kakao.redirect.url}")
    private String redirectUrl;

    @GetMapping("/kakao/login")
    @Operation(summary = "카카오로그인")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        String kakaoUrl = "https://kauth.kakao.com/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + clientKey +
                "&redirect_uri=" + redirectUrl;

        Map<String, String> response = new HashMap<>();
        response.put("redirectUrl", kakaoUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 인가코드 콜백")
    public ResponseEntity<?> getKaKaoAuthorizeCode(@RequestParam("code") String authorizeCode) {
        log.info("[kakao-login] authorizaCode : {}", authorizeCode);
        return authService.getKakaoUserInfo(authorizeCode);
    }

    @PostMapping("/api/v1/auth/kakao")
    @Operation(summary = "카카오 인증")
    public ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginRequest req) {
        log.info("[Kakao SDK Login] AccessToken: {}", req.getAccessToken());
        return authService.kakaoLoginWithSDK(req.getAccessToken());
    }

}
