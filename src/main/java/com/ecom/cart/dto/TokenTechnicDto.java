package com.ecom.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenTechnicDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        String scope
) {}
