package org.example.tryonx.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberInfoDto {
    private String name;
    private String phone;
    private String address;
}
