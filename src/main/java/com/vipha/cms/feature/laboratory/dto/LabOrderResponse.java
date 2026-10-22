package com.vipha.cms.feature.laboratory.dto;

import com.vipha.cms.feature.laboratory.LabOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LabOrderResponse(
        UUID id,
        UUID visitId,
        UUID patientId,
        String doctorId,
        Instant orderedAt,
        LabOrderStatus status,
        String notes,
        List<LabOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {}
