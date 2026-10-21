package com.vipha.cms.feature.prescription.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID visitId,
        UUID patientId,
        String patientName,
        String doctorId,
        String doctorName,
        Instant prescribedAt,
        String notes,
        List<PrescriptionItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
