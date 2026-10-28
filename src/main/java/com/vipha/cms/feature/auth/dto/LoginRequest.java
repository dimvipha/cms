package com.vipha.cms.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Signing in with a password. {@code otp} is the 6-digit code from an authenticator app, needed
 * once the account has two-factor sign-in (staff).
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        @Size(max = 10, message = "The code has at most 10 digits")
        String otp
) {}
