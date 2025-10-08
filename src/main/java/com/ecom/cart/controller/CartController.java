package com.ecom.cart.controller;

import com.ecom.cart.dto.*;
import com.ecom.cart.services.CartService;
import com.ecom.cart.services.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
public class CartController {

    private final CartService cartService;
    private final QrCodeService qrCodeService;

    public CartController(CartService cartService, QrCodeService qrCodeService) {
        this.cartService = cartService;
        this.qrCodeService = qrCodeService;
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

    @PostMapping("/addCart")
    public ResponseEntity<?> addProductToCart(@RequestBody AddProductInCartDto addProductInCartDto) {
        return cartService.addProductToCart(addProductInCartDto);
    }

    @DeleteMapping("/delete-cart/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        boolean delete = cartService.deleteCartById(id);
        if (delete) {
            return ResponseEntity.noContent().build();
        } return ResponseEntity.notFound().build();
    }

    @GetMapping("/qrCode/{id}")
    public ResponseEntity<?> getQrCodeById(@PathVariable Long id) {
        CartItemsDto cartItemsDtoDto = cartService.getQrCodeById(id);
        return ResponseEntity.status(HttpStatus.OK).body(cartItemsDtoDto);
    }

    @GetMapping("/cart-detail/{orderId}")
    public ResponseEntity<?> getCartByOrderId(@PathVariable Long orderId) {
        OrderDto orderDto = cartService.getCartByOrderId(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(orderDto);
    }

    @PostMapping("/decryptQrCode")
    @PreAuthorize("hasAnyAuthority('SCOPE_AGENT', 'SCOPE_ADMIN')")
    public ResponseEntity<QrCodeDto> decryptQrCode(@RequestParam("img") MultipartFile image) throws Exception {
        return ResponseEntity.ok(this.qrCodeService.decryptQrCode(image).getBody());
    }

    @PostMapping("/decryptKeyInQrCode")
    @PreAuthorize("hasAnyAuthority('SCOPE_AGENT', 'SCOPE_ADMIN')")
    public ResponseEntity<DecryptDto> decryptKeyInQrCode(@RequestBody DecryptDto decryptDto) throws Exception {
        return ResponseEntity.ok(this.qrCodeService.decryptKey(decryptDto.getUserId(), decryptDto.getOrderId(), decryptDto.getInputCode()).getBody());
    }


}
