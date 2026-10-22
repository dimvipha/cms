package com.vipha.cms.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** What the frontend got back from Keycloak after the user signed in with Google. */
public record GoogleSignInRequest(
        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "Code verifier is required")
        String codeVerifier,

        @NotBlank(message = "Redirect URI is required")
        String redirectUri
) {}
