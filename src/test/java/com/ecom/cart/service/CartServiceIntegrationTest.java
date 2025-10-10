package com.ecom.cart.service;

import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.clients.ProductRestClient;
import com.ecom.cart.dto.AddProductInCartDto;
import com.ecom.cart.entity.CartItems;
import com.ecom.cart.enums.OrderStatus;
import com.ecom.cart.model.Order;
import com.ecom.cart.model.Product;
import com.ecom.cart.repository.CartRepository;
import com.ecom.cart.services.CartService;
import com.ecom.cart.services.TokenTechnicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private OrderRestClient orderRestClient;

    @MockBean
    private TokenTechnicService tokenTechnicService;

    @MockBean
    private ProductRestClient productRestClient;

    private Order order;
    private Product product;
    private CartItems cartItem;

    @BeforeEach
    void setUp() {
        // Création d'une commande
        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setAmount(100L);
        order.setTotalAmount(100L);
        order.setOrderStatus(OrderStatus.EnCours);

        // Création d'un produit
        product = new Product();
        product.setId(1L);
        product.setName("Produit Test");
        product.setPrice(50L);

        // Création d'un panier
        cartItem = new CartItems();
        cartItem.setId(1L);
        cartItem.setOrderId(order.getId());
        cartItem.setProductId(product.getId());
        cartItem.setUserId(order.getUserId());
        cartItem.setPrice(product.getPrice());
        cartItem.setQuantity(2L);
    }

    // 1 : Ajout d’un produit dans le panier et mise à jour de la commande
    @Test
    void addCaddies_shouldAddProductAndUpdateOrder() {
        AddProductInCartDto dto = new AddProductInCartDto();
        dto.setProductId(product.getId());
        dto.setUserId(order.getUserId());
        dto.setQuantity(2L);

        // On simule le token et la commande active
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");
        when(orderRestClient.findByUserIdAndOrderStatus(anyString(), anyMap())).thenReturn(order);

        // On simule le produit existant
        when(productRestClient.findById(anyString(), eq(product.getId()))).thenReturn(product);

        // On simule qu’aucun produit identique n’existe déjà dans le panier
        when(cartRepository.findByProductIdAndOrderIdAndUserId(product.getId(), order.getId(), order.getUserId()))
                .thenReturn(Optional.empty());

        // On simule l’enregistrement du panier et la mise à jour de la commande
        when(cartRepository.save(any(CartItems.class))).thenReturn(cartItem);
        when(orderRestClient.orderSave(anyString(), any(Order.class))).thenReturn(ResponseEntity.ok().build());

        // Appel de la méthode testée
        ResponseEntity<?> response = cartService.addCaddies(order.getUserId(), List.of(dto));

        // Vérification : statut HTTP OK et sauvegarde du panier
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(cartRepository, times(1)).save(any(CartItems.class));
        verify(orderRestClient, times(1)).orderSave(anyString(), any(Order.class));
    }

    // 2 : Suppression d’un panier existant
    @Test
    void deleteCartById_shouldDeleteCartAndUpdateOrder() {
        Long cartId = 1L;

        // On simule la récupération du panier existant
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cartItem));

        // On simule la récupération de la commande associée
        when(orderRestClient.findById(anyString(), eq(order.getId()))).thenReturn(order);

        // On simule la suppression du panier
        doNothing().when(cartRepository).deleteById(cartId);
        when(orderRestClient.orderSave(anyString(), any(Order.class))).thenReturn(ResponseEntity.ok().build());

        // Appel de la méthode testée
        boolean result = cartService.deleteCartById(cartId);

        // Vérification : suppression réussie et mise à jour de la commande
        assertThat(result).isTrue();
        verify(cartRepository, times(1)).deleteById(cartId);
        verify(orderRestClient, times(1)).orderSave(anyString(), any(Order.class));
    }

    // 3 : Récupération du panier par userId
    @Test
    void getCartByUserId_shouldReturnOrderDto() {
        // On simule le token et la commande active
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");
        when(orderRestClient.findByUserIdAndOrderStatus(anyString(), anyMap())).thenReturn(order);

        // On simule la récupération des articles du panier
        when(cartRepository.findByOrderId(order.getId())).thenReturn(List.of(cartItem));

        // On simule la récupération du produit
        when(productRestClient.findById(anyString(), eq(product.getId()))).thenReturn(product);

        // Appel de la méthode testée
        var result = cartService.getCartByUserId(order.getUserId());

        // Vérification : panier retourné avec informations complètes
        assertThat(result).isNotNull();
        assertThat(result.getCartItems()).hasSize(1);
        assertThat(result.getCartItems().iterator().next().getProductId()).isEqualTo(product.getId());
    }
}
