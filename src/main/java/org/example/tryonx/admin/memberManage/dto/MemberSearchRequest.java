package org.example.tryonx.admin.memberManage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSearchRequest {
    private String searchKey;
    private String searchValue;
}
