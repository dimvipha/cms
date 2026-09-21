package com.vipha.cms.feature.pharmacy.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicineBatchResponse(
        UUID id,
        UUID medicineId,
        String batchNumber,
        LocalDate expiryDate,
        Integer stockQuantity,
        LocalDate receivedAt,
        Instant createdAt
) {}
