package com.vipha.cms.feature.visit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VitalSignResponse(
        UUID id,
        UUID visitId,
        Instant recordedAt,
        String recordedBy,
        String recordedByName,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseRate,
        Integer respiratoryRate,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        BigDecimal heightCm,
        BigDecimal bmi,
        Integer oxygenSaturation
) {}
