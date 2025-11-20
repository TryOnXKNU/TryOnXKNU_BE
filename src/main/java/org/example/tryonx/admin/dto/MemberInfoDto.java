package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.member.domain.Role;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoDto {
    private String profileUrl;
    private String name;
    private Long memberId;
    private String nickname;
    private String phoneNumber;
    private String address;
    private String email;
    private BodyShape bodyShape;
    private Integer height;
    private Integer weight;
    private String memberNum;
    private Long socialId;
    private Role role;
}
