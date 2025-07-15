package org.example.tryonx.admin.dto;

import lombok.*;

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
    private String nickName;
    private String phoneNumber;
    private LocalDate birthday;
    private String address;
    private Integer bodyType;
    private Integer height;
    private Integer weight;
    private Integer gender;
}
