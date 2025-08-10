package org.example.tryonx.member.dto;

import lombok.Getter;
import org.example.tryonx.enums.BodyShape;

@Getter
public class UpdateBodyInfoDto {
    private Integer height;

    private Integer weight;

    private BodyShape bodyShape;
}
