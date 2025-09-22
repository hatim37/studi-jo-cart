package com.ecom.cart.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Product {

    private Long id;
    private String name;
    private Long price;
    private String description;
    private byte[] img;

}
