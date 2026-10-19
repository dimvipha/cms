package com.vipha.cms.feature.visit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record RecordVitalSignRequest(
        @Min(40) @Max(260) Integer systolicBp,
        @Min(20) @Max(200) Integer diastolicBp,
        @Min(20) @Max(250) Integer pulseRate,
        @Min(5) @Max(80) Integer respiratoryRate,
        @DecimalMin("30.0") @DecimalMax("45.0") @Digits(integer = 2, fraction = 1)
        BigDecimal temperatureCelsius,
        @DecimalMin("0.5") @DecimalMax("500.0") @Digits(integer = 3, fraction = 2)
        BigDecimal weightKg,
        @DecimalMin("20.0") @DecimalMax("300.0") @Digits(integer = 3, fraction = 2)
        BigDecimal heightCm,
        @Min(50) @Max(100) Integer oxygenSaturation
) {

    public boolean isEmpty() {
        return systolicBp == null && diastolicBp == null && pulseRate == null
                && respiratoryRate == null && temperatureCelsius == null
                && weightKg == null && heightCm == null && oxygenSaturation == null;
    }
}
