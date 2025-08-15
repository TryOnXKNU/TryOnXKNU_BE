package org.example.tryonx.fitting.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FittingResponse {
    private FittingMemberInfo memberInfo;
    private List<FittingProductInfo> productInfos;
}
