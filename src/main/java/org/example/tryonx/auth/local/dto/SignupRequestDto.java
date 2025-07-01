package org.example.tryonx.auth.local.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String nickname;
    private Integer bodyType;
    private LocalDate birthDate;
    private Integer gender;
    private Integer height;
    private Integer weight;
}
