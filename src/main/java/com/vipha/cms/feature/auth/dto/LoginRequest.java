package com.vipha.cms.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Signing in with a password. */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
