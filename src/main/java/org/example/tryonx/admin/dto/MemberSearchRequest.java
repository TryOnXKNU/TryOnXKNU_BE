package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSearchRequest {
    private String searchKey;
    private String searchValue;
}
