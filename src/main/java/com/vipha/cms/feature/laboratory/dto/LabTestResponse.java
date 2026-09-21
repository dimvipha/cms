package com.vipha.cms.feature.laboratory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LabTestResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        Instant createdAt
) {}
