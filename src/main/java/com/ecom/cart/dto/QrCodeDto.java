package com.ecom.cart.dto;

import lombok.Data;

@Data
public class QrCodeDto {
    private String code;
    private String name;
    private String type;
    private String quantity;
    private String commande;
    private String client;
    private String decrypt;
}
