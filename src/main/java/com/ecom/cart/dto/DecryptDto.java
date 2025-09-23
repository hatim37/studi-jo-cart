package com.ecom.cart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecryptDto {
    private Long userId;
    private Long orderId;
    private String inputCode;
    private String outputCode;

}
