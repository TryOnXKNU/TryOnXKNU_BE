package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskListDto {
    private Long askId;
    private String title;
    private String productName;
    private Size size;
    private String imgUrl;
}
