package com.vipha.cms.feature.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePrescriptionRequest(
        @NotNull UUID visitId,
        String notes,
        @NotEmpty @Valid List<PrescriptionItemRequest> items
) {}
