package org.example.tryonx.member.dto;

import lombok.Data;

@Data
public class UpdateMemberRequestDto {
    private String nickname;

    private String address;

    private String newPassword;

    private Integer height;

    private Integer weight;

    private Integer bodyType;
}
