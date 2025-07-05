package org.example.tryonx.kakao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoProfile {
    private Long id;
    private String nickname;
    private String email;
    private String profile_image;
    private String gender;
    private String birthday;
    private String birthyear;
    private String phone_number;
    private String shipping_address;
}

