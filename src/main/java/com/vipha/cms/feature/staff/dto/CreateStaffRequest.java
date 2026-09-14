package com.vipha.cms.feature.staff.dto;

import com.vipha.cms.feature.staff.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotNull
        StaffType role,

        @Size(min = 12, message = "Initial password must be at least 12 characters")
        String initialPassword
) {}
