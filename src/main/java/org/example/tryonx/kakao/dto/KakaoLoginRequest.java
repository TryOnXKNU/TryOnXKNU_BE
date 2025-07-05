package org.example.tryonx.kakao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoLoginRequest {
    private String accessToken;
    private KakaoProfile kakaoProfile;
}
