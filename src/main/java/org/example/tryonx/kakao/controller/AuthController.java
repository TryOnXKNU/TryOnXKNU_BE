package org.example.tryonx.kakao.controller;

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
public class AuthController {
    private final AuthService authService;

    @Value("${kakao.client.id}")
    private String clientKey;
    @Value("${kakao.redirect.url}")
    private String redirectUrl;

    @GetMapping("/kakao/login")
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
    public ResponseEntity<?> getKaKaoAuthorizeCode(@RequestParam("code") String authorizeCode) {
        log.info("[kakao-login] authorizaCode : {}", authorizeCode);
        return authService.getKakaoUserInfo(authorizeCode);
    }

    @PostMapping("/api/v1/auth/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        log.info("[Kakao SDK Login] AccessToken: {}", request.getAccessToken());
        return authService.kakaoLoginWithSDK(request.getAccessToken(), request.getKakaoProfile());
    }
}
