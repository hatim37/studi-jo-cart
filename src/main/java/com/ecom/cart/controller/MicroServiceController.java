package com.ecom.cart.controller;

import com.ecom.cart.entity.CartItems;
import com.ecom.cart.services.CartService;
import com.ecom.cart.services.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MicroServiceController {
    private final QrCodeService qrCodeService;
    private final CartService cartService;

    public MicroServiceController(QrCodeService qrCodeService, CartService cartService) {
        this.qrCodeService = qrCodeService;
        this.cartService = cartService;
    }

    @PostMapping("/_internal/cartItems-qrCode")
    public void generateQrCode(@RequestBody Map<String, Long> qrCode){
        this.qrCodeService.generateQrCode(qrCode.get("userId"), qrCode.get("orderId"));
    }

    @GetMapping("/_internal/allCartByQrCodeIsNotNull")
    public List<CartItems> findByQrCodeIsNotNull(){
        return this.cartService.findByQrCodeIsNotNull();
    }

}

