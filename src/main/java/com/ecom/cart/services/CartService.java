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
import com.ecom.cart.repository.CartRepository;
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

    private final CartRepository cartRepository;
    private final OrderRestClient orderRestClient;
    private final TokenTechnicService tokenTechnicService;
    private final ProductRestClient productRestClient;

    public CartService(CartRepository cartRepository, OrderRestClient orderRestClient, TokenTechnicService tokenTechnicService, ProductRestClient productRestClient) {
        this.cartRepository = cartRepository;
        this.orderRestClient = orderRestClient;
        this.tokenTechnicService = tokenTechnicService;
        this.productRestClient = productRestClient;
    }

    public ResponseEntity<?> addCaddies(Long userId, List<AddProductInCartDto> addProductInCartDto) {
        Order activeOrder = orderRestClient.findByUserIdAndOrderStatus("Bearer " + this.tokenTechnicService.getTechnicalToken(), Map.of("userId", String.valueOf(userId), "orderStatus", String.valueOf(OrderStatus.EnCours)));
        if (activeOrder.getId() == null) {
            throw new UserNotFoundException("Service indisponible order");
        }

        for (AddProductInCartDto dto : addProductInCartDto) {
            //on vérifie si le produit est déjà dans le panier
            Optional<CartItems> optionalCartItems = cartRepository.findByProductIdAndOrderIdAndUserId
                    (dto.getProductId(), activeOrder.getId(), dto.getUserId());

            if (optionalCartItems.isPresent()) {
                CartItems cartItems = optionalCartItems.get();
                cartItems.setQuantity(cartItems.getQuantity() + dto.getQuantity());
                cartRepository.save(cartItems);

                activeOrder.setTotalAmount(activeOrder.getTotalAmount() + cartItems.getPrice() * dto.getQuantity());
                activeOrder.setAmount(activeOrder.getAmount() + cartItems.getPrice() * dto.getQuantity());

            } else {

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

            cartRepository.save(cartItems);
            activeOrder.setTotalAmount(activeOrder.getTotalAmount() + cartItems.getPrice() * dto.getQuantity());
            activeOrder.setAmount(activeOrder.getAmount() + cartItems.getPrice() * dto.getQuantity());}
        }

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
        log.info("userId = " + userId+" demande cart");
        Order activeOrder = this.orderRestClient.findByUserIdAndOrderStatus("Bearer " + this.tokenTechnicService.getTechnicalToken(), Map.of("userId", String.valueOf(userId), "orderStatus", String.valueOf(OrderStatus.EnCours)));
        if (activeOrder.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }
        log.info("retour activeOrder = " + activeOrder);
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

    public ResponseEntity<?> addProductToCart(AddProductInCartDto addProductInCartDto) {
        Order activeOrder = orderRestClient.findByUserIdAndOrderStatus("Bearer " + this.tokenTechnicService.getTechnicalToken(), Map.of("userId", String.valueOf(addProductInCartDto.getUserId()), "orderStatus", String.valueOf(OrderStatus.EnCours)));
        if (activeOrder.getId() == null) {
            throw new UserNotFoundException("Service indisponible order");
        }

        Optional<CartItems> optionalCartItems = cartRepository.findByProductIdAndOrderIdAndUserId
                (addProductInCartDto.getProductId(), activeOrder.getId(), addProductInCartDto.getUserId());

        if (optionalCartItems.isPresent() && Objects.equals(addProductInCartDto.getOption(), "add")) {

            CartItems cartItems = cartRepository.findById(optionalCartItems.get().getId()).orElseThrow();
            cartItems.setQuantity(cartItems.getQuantity() + addProductInCartDto.getQuantity());
            cartRepository.save(cartItems);

            activeOrder.setTotalAmount(activeOrder.getTotalAmount() + cartItems.getPrice() * addProductInCartDto.getQuantity());
            activeOrder.setAmount(activeOrder.getAmount() + cartItems.getPrice() * addProductInCartDto.getQuantity());

            return this.sendUpdateOrders(activeOrder);

        }
        if (optionalCartItems.isPresent() && Objects.equals(addProductInCartDto.getOption(), "remove")) {
            CartItems cartItems = cartRepository.findById(optionalCartItems.get().getId()).orElseThrow();
            cartItems.setQuantity(cartItems.getQuantity() - addProductInCartDto.getQuantity());
            cartRepository.save(cartItems);

            activeOrder.setTotalAmount(activeOrder.getTotalAmount() - cartItems.getPrice() * addProductInCartDto.getQuantity());
            activeOrder.setAmount(activeOrder.getAmount() - cartItems.getPrice() * addProductInCartDto.getQuantity());

            return this.sendUpdateOrders(activeOrder);

        } else {

            Product optionalProduct = productRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(), addProductInCartDto.getProductId());
            if (optionalProduct.getId() == null) {
                throw new UserNotFoundException("Produit introuvable");
            }

            CartItems cartItems = new CartItems();
            cartItems.setProductId(optionalProduct.getId());
            cartItems.setOrderId(activeOrder.getId());
            cartItems.setUserId(addProductInCartDto.getUserId());
            cartItems.setPrice(optionalProduct.getPrice());
            cartItems.setQuantity(addProductInCartDto.getQuantity());

            cartRepository.save(cartItems);

            activeOrder.setTotalAmount(activeOrder.getTotalAmount() + cartItems.getPrice() * addProductInCartDto.getQuantity());
            activeOrder.setAmount(activeOrder.getAmount() + cartItems.getPrice() * addProductInCartDto.getQuantity());

            return this.sendUpdateOrders(activeOrder);
        }
    }

    public boolean deleteCartById(Long id) {
        Optional<CartItems> cartItems = cartRepository.findById(id);
        if (cartItems.isPresent()) {
            cartRepository.deleteById(id);
            return true;
        } return false;
    }

    public CartItemsDto getQrCodeById(Long id) {
        CartItems cartItems = cartRepository.findById(id).get();
        CartItemsDto cartItemsDto = new CartItemsDto();
        cartItemsDto.setQrCode(cartItems.getQrCode());
        return cartItemsDto;
    }

    public OrderDto getCartByOrderId(Long orderId) {
        Order order = orderRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(), orderId);
        if (order.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }

        List<CartItemsDto> dtos = cartRepository.findByOrderId(orderId).stream()
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
        orderDto.setId(order.getId());
        orderDto.setUserId(order.getUserId());
        orderDto.setCartItems(dtos);

        return orderDto;
    }


    public List<CartItems> findByQrCodeIsNotNull() {
        return cartRepository.findByQrCodeIsNotNull();
    }
}


