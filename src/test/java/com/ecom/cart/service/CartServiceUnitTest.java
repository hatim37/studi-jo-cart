package com.ecom.cart.service;


import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.dto.AddProductInCartDto;
import com.ecom.cart.enums.OrderStatus;
import com.ecom.cart.model.Product;
import com.ecom.cart.repository.CartRepository;
import com.ecom.cart.response.UserNotFoundException;
import com.ecom.cart.services.CartService;
import com.ecom.cart.services.TokenTechnicService;
import com.ecom.cart.clients.ProductRestClient;
import com.ecom.cart.model.Order;
import com.ecom.cart.entity.CartItems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceUnitTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRestClient orderRestClient;

    @Mock
    private TokenTechnicService tokenTechnicService;

    @Mock
    private ProductRestClient productRestClient;

    private Order order;
    private Product product;
    private CartItems cartItem;

    @BeforeEach
    void setUp() {
        // Création d'une commande fictive
        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setAmount(100L);
        order.setTotalAmount(100L);
        order.setOrderStatus(OrderStatus.EnCours);

        // Création d'un produit fictif
        product = new Product();
        product.setId(1L);
        product.setName("Produit Test");
        product.setPrice(50L);

        // Création d'un panier fictif
        cartItem = new CartItems();
        cartItem.setId(1L);
        cartItem.setOrderId(order.getId());
        cartItem.setProductId(product.getId());
        cartItem.setUserId(order.getUserId());
        cartItem.setPrice(product.getPrice());
        cartItem.setQuantity(2L);
    }

    // 1 : Ajout d’un produit avec succès
    @Test
    void addCaddies_shouldAddProductSuccessfully() {
        AddProductInCartDto dto = new AddProductInCartDto();
        dto.setProductId(product.getId());
        dto.setUserId(order.getUserId());
        dto.setQuantity(2L);

        // On simule la récupération du token et de la commande active
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");
        when(orderRestClient.findByUserIdAndOrderStatus(anyString(), anyMap())).thenReturn(order);

        // On simule la récupération du produit
        when(productRestClient.findById(anyString(), eq(product.getId()))).thenReturn(product);

        // On simule qu’aucun produit identique n’existe déjà dans le panier
        when(cartRepository.findByProductIdAndOrderIdAndUserId(product.getId(), order.getId(), order.getUserId()))
                .thenReturn(Optional.empty());

        // On simule l’enregistrement du panier et la mise à jour de la commande
        when(cartRepository.save(any(CartItems.class))).thenReturn(cartItem);
        when(orderRestClient.orderSave(anyString(), any(Order.class))).thenReturn(ResponseEntity.ok().build());

        // Appel de la méthode testée
        ResponseEntity<?> response = cartService.addCaddies(order.getUserId(), List.of(dto));

        // Vérification : statut HTTP OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Vérification : sauvegarde du panier et mise à jour de la commande
        verify(cartRepository, times(1)).save(any(CartItems.class));
        verify(orderRestClient, times(1)).orderSave(anyString(), any(Order.class));
    }

    // 2 : Ajout d’un produit  => quantité mise à jour
    @Test
    void addCaddies_shouldThrowWhenOrderNotFound() {
        AddProductInCartDto dto = new AddProductInCartDto();
        dto.setUserId(1L);

        // On simule une commande inexistante (id null)
        when(orderRestClient.findByUserIdAndOrderStatus(anyString(), anyMap()))
                .thenReturn(new Order());

        // Vérification : exception levée
        assertThrows(UserNotFoundException.class,
                () -> cartService.addCaddies(1L, List.of(dto)));
    }

    // 3 : Ajout d’un produit, exception si commande introuvable
    @Test
    void addCaddies_shouldThrowWhenProductNotFound() {
        AddProductInCartDto dto = new AddProductInCartDto();
        dto.setUserId(order.getUserId());
        dto.setProductId(99L);

        // On simule le token et la commande existante
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");
        when(orderRestClient.findByUserIdAndOrderStatus(anyString(), anyMap())).thenReturn(order);

        // On simule un produit inexistant (id null)
        when(productRestClient.findById(anyString(), eq(99L))).thenReturn(new Product());

        // Vérification : exception levée
        assertThrows(UserNotFoundException.class,
                () -> cartService.addCaddies(order.getUserId(), List.of(dto)));
    }
}

