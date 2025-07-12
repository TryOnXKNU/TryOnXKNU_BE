package org.example.tryonx.ask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskListItem {
    private String productName;
    private Size size;
    private String imgUrl;
}
