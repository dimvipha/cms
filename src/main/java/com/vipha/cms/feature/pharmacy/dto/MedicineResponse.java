package com.vipha.cms.feature.pharmacy.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MedicineResponse(
        UUID id,
        String code,
        String name,
        String unit,
        String form,
        BigDecimal price,
        Integer reorderLevel,
        Integer totalStock,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
