package com.ecom.cart.controller;

import com.ecom.cart.dto.*;
import com.ecom.cart.services.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/addCaddy/{userId}")
    public ResponseEntity<?> addCaddies(@PathVariable Long userId,@RequestBody List<AddProductInCartDto> addProductInCartDto) {
        return cartService.addCaddies(userId,addProductInCartDto);
    }

    @GetMapping("/cart/{userId}")
    public ResponseEntity<?> getCartByUserId(@PathVariable Long userId) {
        OrderDto orderDto = cartService.getCartByUserId(userId);
        return ResponseEntity.status(HttpStatus.OK).body(orderDto);
    }


}
