package org.example.tryonx.apple.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AppleDTO {
    private String id;
    private String token;
    private String email;
}
