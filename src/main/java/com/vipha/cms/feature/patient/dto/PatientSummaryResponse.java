package com.vipha.cms.feature.patient.dto;

import java.util.UUID;

public record PatientSummaryResponse(
        UUID id,
        String mrn,
        String firstName,
        String lastName,
        Integer age,
        String gender,
        String bloodGroup,
        String allergies
) {}
