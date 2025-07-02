package org.example.tryonx.kakao.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> getKakaoUserInfo(String authorizeCode);

}
