package org.example.tryonx.admin.dto;

import lombok.*;
import org.example.tryonx.member.domain.Role;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListDto {
    private String profileUrl;
    private Long memberId;
//    private String name;
    private String nickname;
    private String memberNum;
    private Long socialId;
    private Role role;
}
