package com.vipha.cms.feature.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("refresh_expires_in")
        Long refreshExpiresIn,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        String scope
) {}
