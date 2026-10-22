package com.vipha.cms.feature.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMedicineRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank String name,
        @NotBlank @Size(max = 30) String unit,
        @Size(max = 30) String form,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer reorderLevel
) {}
