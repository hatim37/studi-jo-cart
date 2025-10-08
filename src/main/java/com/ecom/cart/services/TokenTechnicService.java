package com.ecom.cart.services;

import com.ecom.cart.clients.SecurityRestClient;
import com.ecom.cart.dto.TokenTechnicDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Service
public class TokenTechnicService {

    private final SecurityRestClient securityRestClient;
    @Value("${client.id}")
    private  String clientId;
    @Value("${client.secret}")
    private  String clientSecret;

    public TokenTechnicService(SecurityRestClient securityRestClient) {
        this.securityRestClient = securityRestClient;
    }


    public String getTechnicalToken() {
        // 1) Calcul du header Basic
        String creds = clientId+":"+clientSecret;
        String basicAuth = "Basic " +
                Base64.getEncoder().encodeToString(
                        creds.getBytes(StandardCharsets.UTF_8)
                );

        // 2) Construction manuelle du corps form-url-encoded
        String form = "grant_type=client_credentials&scope=users:read";

        // 3) Appel Feign
        TokenTechnicDto resp = securityRestClient.getTokenTechnic(basicAuth, form);
        // 4) Retourne l’access_token (ou null si fallback)
        return resp == null ? null : resp.accessToken();
    }
}
