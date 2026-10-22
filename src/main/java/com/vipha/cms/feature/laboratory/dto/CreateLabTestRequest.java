package com.vipha.cms.feature.laboratory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLabTestRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {}
