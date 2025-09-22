package com.ecom.cart.model;

import com.ecom.cart.dto.OrderDto;
import com.ecom.cart.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@ToString
public class Order {

    private Long id;
    private String orderDescription;
    private Date date;
    private Long amount;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private Long totalAmount;
    private UUID trackingId;
    @Transient
    private User user;
    private Long userId;
    private String secretKey;

    public OrderDto getOrderDto(){
        OrderDto orderDto = new OrderDto();
        orderDto.setId(id);
        orderDto.setOrderDescription(orderDescription);
        orderDto.setAmount(amount);
        orderDto.setDate(date);
        orderDto.setOrderStatus(orderStatus);
        orderDto.setTrackingId(trackingId);
        orderDto.setOrderStatus(orderStatus);
        return orderDto;
    }


}
