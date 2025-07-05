package org.example.tryonx.kakao.service;

import org.example.tryonx.kakao.dto.KakaoProfile;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> getKakaoUserInfo(String authorizeCode);
    ResponseEntity<?> kakaoLoginWithSDK(String accessToken, KakaoProfile profile);

}
