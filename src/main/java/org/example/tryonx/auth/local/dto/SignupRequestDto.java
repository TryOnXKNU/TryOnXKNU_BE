package org.example.tryonx.auth.local.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.BodyShape;

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
    private BodyShape bodyShape;
    private LocalDate birthDate;
    private Integer height;
    private Integer weight;
}
