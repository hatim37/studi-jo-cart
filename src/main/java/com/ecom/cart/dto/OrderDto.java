package com.ecom.cart.dto;


import com.ecom.cart.enums.OrderStatus;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Data
public class OrderDto {

    private Long id;
    private String orderDescription;
    private Date date;
    private Long amount;
    private OrderStatus orderStatus;
    private Long totalAmount;
    private Long discount;
    private UUID trackingId;
    private Long userId;
    private Collection<CartItemsDto> cartItems;

}
