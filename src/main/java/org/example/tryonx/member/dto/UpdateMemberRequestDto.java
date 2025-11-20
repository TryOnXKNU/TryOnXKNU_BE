package org.example.tryonx.member.dto;

import lombok.Data;
import org.example.tryonx.enums.BodyShape;

@Data
public class UpdateMemberRequestDto {
    private String nickname;
    private String name;

    private String address;

    private String newPassword;

    private Integer height;

    private Integer weight;

    private BodyShape bodyShape;
    private String phoneNumber;
}
