package com.vipha.cms.feature.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMedicineRequest(
        String name,
        @Size(max = 30) String unit,
        @Size(max = 30) String form,
        @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer reorderLevel,
        Boolean active
) {}
