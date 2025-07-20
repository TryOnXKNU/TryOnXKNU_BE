package org.example.tryonx.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.Size;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    private Long cartItemId;
    private Integer productId;
    private Integer productItemId;
    private String productName;
    private Size size;
    private Integer quantity;
    private BigDecimal price;
    private String imageUrl;
    private List<Size> availableSizes;
}
