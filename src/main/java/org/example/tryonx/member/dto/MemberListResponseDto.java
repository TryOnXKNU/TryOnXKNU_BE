package org.example.tryonx.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberListResponseDto {
    private String name;
    private String email;
    private String phone;
}
