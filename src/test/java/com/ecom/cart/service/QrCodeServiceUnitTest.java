package com.ecom.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.clients.UserRestClient;
import com.ecom.cart.model.Order;
import com.ecom.cart.model.User;

import com.ecom.cart.response.UserNotFoundException;
import com.ecom.cart.services.QrCodeService;
import com.ecom.cart.services.TokenTechnicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

class QrCodeServiceUnitTest {

    @InjectMocks
    private QrCodeService qrCodeService;

    @Mock
    private UserRestClient userRestClient;

    @Mock
    private OrderRestClient orderRestClient;

    @Mock
    private TokenTechnicService tokenTechnicService;

    private User user;
    private Order order;

    @BeforeEach
    void setUp() {
        // Initialisation
        MockitoAnnotations.openMocks(this);

        // Création d’un utilisateur avec clé secrète
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSecretKey(Base64.getEncoder().encodeToString("userKey12345678".getBytes()));

        // Création d’une commande avec clé secrète
        order = new Order();
        order.setId(1L);
        order.setUserId(user.getId());
        order.setSecretKey(Base64.getEncoder().encodeToString("orderKey12345678".getBytes()));
    }

    // 1 : Test de génération de la clé secrète combinée
    @Test
    void getKeyFormUserAndOrder_shouldReturnSecretKey() throws Exception {
        // On simule le token
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");

        // On simule la récupération des clés de l’utilisateur et de la commande
        when(userRestClient.findUserById(anyString(), eq(user.getId()))).thenReturn(user);
        when(orderRestClient.findById(anyString(), eq(order.getId()))).thenReturn(order);

        // Appel de la méthode testée
        SecretKeySpec key = qrCodeService.getKeyFormUserAndOrder(user.getId(), order.getId());

        // Vérification, clé non nulle et type AES
        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("AES");
    }

    // 2 : Test levée UserNotFoundException si utilisateur inexistant
    @Test
    void getKeyFormUserAndOrder_shouldThrowUserNotFound() {
        // On simule le token
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");

        // On simule utilisateur sans ID
        when(userRestClient.findUserById(anyString(), eq(user.getId()))).thenReturn(new User());

        // Vérification, exception levée avec message
        assertThatThrownBy(() -> qrCodeService.getKeyFormUserAndOrder(user.getId(), order.getId()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    // 3 : Test levée UserNotFoundException si commande inexistante
    @Test
    void getKeyFormUserAndOrder_shouldThrowOrderNotFound() {
        // On simule le token
        when(tokenTechnicService.getTechnicalToken()).thenReturn("token");

        // On simule utilisateur valide
        when(userRestClient.findUserById(anyString(), eq(user.getId()))).thenReturn(user);

        // On simule commande inexistante
        when(orderRestClient.findById(anyString(), eq(order.getId()))).thenReturn(new Order());

        // Vérification, exception levée avec message
        assertThatThrownBy(() -> qrCodeService.getKeyFormUserAndOrder(user.getId(), order.getId()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("N° de commande introuvable");
    }
}