package com.vipha.cms.feature.patient.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        LocalDate dob,
        Integer age,
        String gender,
        String phone,
        String email,
        String address,
        String bloodGroup,
        String allergies,
        String emergencyContactName,
        String emergencyContactPhone,
        Instant createdAt
) {}