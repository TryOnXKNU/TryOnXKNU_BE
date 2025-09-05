package org.example.tryonx.admin.dto;

import lombok.*;

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
}
