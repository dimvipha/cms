package com.vipha.cms.feature.prescription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PrescriptionItemRequest(
        @NotNull UUID medicineId,
        @NotBlank String dosage,
        @NotBlank String frequency,
        @NotBlank String duration,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 500) String instructions
) {}
