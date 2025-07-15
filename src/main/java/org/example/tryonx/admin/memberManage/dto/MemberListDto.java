package org.example.tryonx.admin.memberManage.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListDto {
    private String profileUrl;
    private Long memberId;
    private String name;
}
