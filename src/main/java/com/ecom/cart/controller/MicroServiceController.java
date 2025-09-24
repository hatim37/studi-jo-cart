package com.ecom.cart.controller;

import com.ecom.cart.services.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class MicroServiceController {
    private final QrCodeService qrCodeService;

    public MicroServiceController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/_internal/cartItems-qrCode")
    public void generateQrCode(@RequestBody Map<String, Long> qrCode){
        this.qrCodeService.generateQrCode(qrCode.get("userId"), qrCode.get("orderId"));
    }

}

