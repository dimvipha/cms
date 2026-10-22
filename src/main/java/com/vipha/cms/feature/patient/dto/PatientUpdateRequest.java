package com.vipha.cms.feature.patient.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PatientUpdateRequest(
        @Size(max = 60)
        String firstName,
        @Size(max = 60)
        String lastName,
        @Past
        LocalDate dob,
        String gender,
        @Pattern(regexp = "^\\+?[0-9]{8,15}$",
                message = "Phone must be 8-15 digits, optionally starting with +")
        String phone,
        @Email
        String email,
        @Size(max = 255)
        String address,
        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood group")
        String bloodGroup,
        String allergies,
        String emergencyContactName,
        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String emergencyContactPhone
) {}
