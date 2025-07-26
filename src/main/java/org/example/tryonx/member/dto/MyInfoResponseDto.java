package org.example.tryonx.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyInfoResponseDto {
    private String nickname;

    private String profileImage;

    private String phoneNumber;

    private LocalDate birthDate;

    private String address;

    private String email;

    private Integer bodyType;

    private Integer height;

    private Integer weight;

    private Integer point;
}
