package com.vipha.cms.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "Confirmed password is required")
        String confirmedPassword,
        // proving the current password is a sign-in, so two-factor accounts give their code too
        @Size(max = 10, message = "The code has at most 10 digits")
        String otp
) {}
