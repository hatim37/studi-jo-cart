package com.ecom.cart.services;


import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.clients.ProductRestClient;
import com.ecom.cart.dto.AddProductInCartDto;
import com.ecom.cart.dto.CartItemsDto;
import com.ecom.cart.dto.OrderDto;
import com.ecom.cart.entity.CartItems;
import com.ecom.cart.enums.OrderStatus;
import com.ecom.cart.model.Order;
import com.ecom.cart.model.Product;
import com.ecom.cart.repository.CartIRepository;
import com.ecom.cart.response.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class CartService {

    private final CartIRepository cartRepository;
    private final OrderRestClient orderRestClient;
    private final TokenTechnicService tokenTechnicService;
    private final ProductRestClient productRestClient;

    public CartService(CartIRepository cartRepository, OrderRestClient orderRestClient, TokenTechnicService tokenTechnicService, ProductRestClient productRestClient) {
        this.cartRepository = cartRepository;
        this.orderRestClient = orderRestClient;
        this.tokenTechnicService = tokenTechnicService;
        this.productRestClient = productRestClient;
    }

    public ResponseEntity<?> addCaddies(Long userId, List<AddProductInCartDto> addProductInCartDto) {
        log.info("addCaddies debut");
        Order activeOrder = orderRestClient.findByUserIdAndOrderStatus("Bearer " + this.tokenTechnicService.getTechnicalToken(), Map.of("userId", String.valueOf(userId), "orderStatus", String.valueOf(OrderStatus.EnCours)));
        if (activeOrder.getId() == null) {
            throw new UserNotFoundException("Service indisponible order");
        }

        log.info("addCaddies boucle");
        for (AddProductInCartDto dto : addProductInCartDto) {

            log.info("addCaddies recherche produit");
            Product product = productRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(), dto.getProductId());
            if (product.getId() == null) {
                throw new UserNotFoundException("Produit introuvable");
            }
            CartItems cartItems = new CartItems();
            cartItems.setOrderId(activeOrder.getId());
            cartItems.setProductId(dto.getProductId());
            cartItems.setPrice(product.getPrice());
            cartItems.setQuantity(dto.getQuantity());
            cartItems.setUserId(dto.getUserId());

            log.info("addCaddies panier creer");
            cartRepository.save(cartItems);
            activeOrder.setTotalAmount(activeOrder.getTotalAmount() + cartItems.getPrice() * dto.getQuantity());
            activeOrder.setAmount(activeOrder.getAmount() + cartItems.getPrice() * dto.getQuantity());
        }
        log.info("addCaddies fin");
        return this.sendUpdateOrders(activeOrder);
    }

    public ResponseEntity<?> sendUpdateOrders(Order order){
        ResponseEntity<Void> resp = this.orderRestClient.orderSave("Bearer " + this.tokenTechnicService.getTechnicalToken(), order);
        if (resp.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(Map.of("message", "Produit ajouté dans le panier "), HttpStatus.OK);
        } else {
            throw new UserNotFoundException("Service indisponible");
        }
    }

    public OrderDto getCartByUserId(Long userId) {
        Order activeOrder = this.orderRestClient.findByUserIdAndOrderStatus("Bearer " + this.tokenTechnicService.getTechnicalToken(), Map.of("userId", String.valueOf(userId), "orderStatus", String.valueOf(OrderStatus.EnCours)));
        if (activeOrder.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }

        List<CartItemsDto> dtos = cartRepository.findByOrderId(activeOrder.getId()).stream()
                .map(item -> {
                    CartItemsDto dto = new CartItemsDto();
                    dto.setId(item.getId());
                    dto.setPrice(item.getPrice());
                    dto.setQuantity(item.getQuantity());
                    dto.setProductId(item.getProductId());
                    dto.setOrderId(item.getOrderId());
                    dto.setUserId(item.getUserId());
                    dto.setQrCode(item.getQrCode());

                    Product prod = productRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(), dto.getProductId());
                    if (prod.getId() == null) {
                        throw new UserNotFoundException("Service indisponible");
                    }
                    dto.setProductName(prod.getName());
                    dto.setReturnedImg(prod.getImg());

                    return dto;
                })
                .toList();

        OrderDto orderDto = new OrderDto();
        orderDto.setAmount(activeOrder.getAmount());
        orderDto.setId(activeOrder.getId());
        orderDto.setOrderStatus(activeOrder.getOrderStatus());
        orderDto.setTotalAmount(activeOrder.getTotalAmount());
        orderDto.setUserId(activeOrder.getUserId());
        orderDto.setTrackingId(activeOrder.getTrackingId());
        orderDto.setCartItems(dtos);

        return orderDto;
    }




}


