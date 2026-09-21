package com.vipha.cms.feature.visit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record RecordVitalSignRequest(
        @Min(40) @Max(260) Integer systolicBp,
        @Min(20) @Max(200) Integer diastolicBp,
        @Min(20) @Max(250) Integer pulseRate,
        @Min(5) @Max(80) Integer respiratoryRate,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        BigDecimal heightCm,
        @Min(0) @Max(100) Integer oxygenSaturation
) {}
