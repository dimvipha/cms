package com.vipha.cms.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        String username,
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "first name is required")
        String firstName,
        @NotBlank(message = "last name is required")
        String lastName,
        @NotBlank(message = "password is required")
        String password,
        @NotBlank(message = "confirm password is required")
        String confirmedPassword
) {
}