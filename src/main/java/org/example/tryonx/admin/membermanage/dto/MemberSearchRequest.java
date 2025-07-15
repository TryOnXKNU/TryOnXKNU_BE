package org.example.tryonx.admin.membermanage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSearchRequest {
    private String searchKey;
    private String searchValue;
}
