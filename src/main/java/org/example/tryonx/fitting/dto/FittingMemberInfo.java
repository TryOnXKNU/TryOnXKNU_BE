package org.example.tryonx.fitting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.BodyShape;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FittingMemberInfo {
    private Integer height;
    private Integer weight;
    private BodyShape bodyShape;
}
