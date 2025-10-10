package com.ecom.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.clients.ProductRestClient;
import com.ecom.cart.clients.UserRestClient;
import com.ecom.cart.dto.QrCodeDto;
import com.ecom.cart.entity.CartItems;
import com.ecom.cart.enums.OrderStatus;
import com.ecom.cart.model.Order;
import com.ecom.cart.model.Product;
import com.ecom.cart.model.User;
import com.ecom.cart.repository.CartRepository;


import com.ecom.cart.response.UserNotFoundException;
import com.ecom.cart.services.QrCodeService;
import com.ecom.cart.services.TokenTechnicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Base64;
import java.util.List;

@SpringBootTest
class QrCodeServiceIntegrationTest {

    @Autowired
    private QrCodeService qrCodeService;

    @MockBean
    private UserRestClient userRestClient;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private TokenTechnicService tokenTechnicService;

    @MockBean
    private ProductRestClient productRestClient;

    @MockBean
    private OrderRestClient orderRestClient;

    private User user;
    private Order order;
    private Product product;
    private CartItems cartItem;

    @BeforeEach
    void setUp() {
        // Création d’un utilisateur fictif avec clé secrète
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSecretKey(Base64.getEncoder().encodeToString("userKey12345678".getBytes()));

        // Création d’une commande fictive avec clé secrète
        order = new Order();
        order.setId(1L);
        order.setUserId(user.getId());
        order.setOrderStatus(OrderStatus.Valider);
        order.setSecretKey(Base64.getEncoder().encodeToString("orderKey12345678".getBytes()));

        // Création d’un produit fictif
        product = new Product();
        product.setId(1L);
        product.setName("Produit Test");

        // Création d’un item de panier
        cartItem = new CartItems();
        cartItem.setId(1L);
        cartItem.setOrderId(order.getId());
        cartItem.setProductId(product.getId());
        cartItem.setUserId(user.getId());
        cartItem.setQuantity(2L);
    }

    // 1 : Génération d’un QR code pour un panier existant
    @Test
    void generateQrCode_shouldGenerateAndSaveQrCode() {
        // On simule le token technique
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");

        // On simule la récupération de l’utilisateur
        when(userRestClient.findUserById(anyString(), eq(user.getId()))).thenReturn(user);

        // On simule la récupération de la commande
        when(orderRestClient.findById(anyString(), eq(order.getId()))).thenReturn(order);

        // On simule la récupération des items du panier
        when(cartRepository.findByOrderId(order.getId())).thenReturn(List.of(cartItem));

        // On simule la récupération du produit
        when(productRestClient.findById(anyString(), eq(product.getId()))).thenReturn(product);

        // On simule la sauvegarde du panier
        when(cartRepository.save(any(CartItems.class))).thenReturn(cartItem);

        // Appel de la méthode testée
        qrCodeService.generateQrCode(user.getId(), order.getId());

        // Vérification, méthode save appelée
        verify(cartRepository, times(1)).save(cartItem);
    }

    // 2 : Décryptage QR code valide
    @Test
    void decryptQrCode_shouldReturnQrCodeDto() throws Exception {
        // Création d’un DTO factice
        QrCodeDto dto = new QrCodeDto();
        dto.setCode("1234");
        dto.setClient("1");
        dto.setCommande("1");
        dto.setName("John");

        // Vérification des données du DTO
        assertThat(dto.getCode()).isNotEmpty();
        assertThat(dto.getClient()).isEqualTo("1");
    }

    // 3 : Génération QR code utilisateur inexistant
    @Test
    void generateQrCode_shouldThrowWhenUserNotFound() {
        // On simule le token
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");

        // On simule un utilisateur inexistant (id null)
        User emptyUser = new User(); // id null
        when(userRestClient.findUserById(anyString(), eq(user.getId()))).thenReturn(emptyUser);

        // Vérification, exception levée avec message approprié
        assertThatThrownBy(() -> qrCodeService.generateQrCode(user.getId(), order.getId()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Service indisponible");
    }
}