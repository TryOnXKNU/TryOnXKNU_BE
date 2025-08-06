package org.example.tryonx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskListDto {
    private Long askId;
    private String title;
    private String content;
    private String productName;
    private Size size;
    private String imgUrl;
    private List<String> askImageUrls;
}
