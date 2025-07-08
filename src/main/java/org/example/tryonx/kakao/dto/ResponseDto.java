package org.example.tryonx.kakao.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDto {
    private Long kakaoId;
    private String name;
    private String email;
    private String profile_image;
    private String profile_nickname;
    private String birthday;
    private String birthyear;
    private String phone_number;
    private String shipping_address;
}
