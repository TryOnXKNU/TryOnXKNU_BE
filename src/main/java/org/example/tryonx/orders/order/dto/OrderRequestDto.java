package org.example.tryonx.orders.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tryonx.enums.Size;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class OrderRequestDto {
    @NotNull
    private List<Item> items = new ArrayList<>();

    @NotNull @Min(0)
    private Integer point = 0;

    private String deliveryRequest;

    @NotBlank
    private String merchantUid;

    @Getter @Setter
    @NoArgsConstructor
    public static class Item {
        @NotNull private Integer productId;
        @NotNull private Size size;   // Enum이면 JSON과 이름 정확히 맞춰야 함 ("M" vs "MEDIUM")
        @NotNull @Min(1) private Integer quantity;
        private Long cartItemId;
    }
}

