package com.vipha.cms.feature.laboratory.dto;

import com.vipha.cms.feature.laboratory.LabOrderStatus;

import java.time.Instant;
import java.util.UUID;

public record LabOrderItemResponse(
        UUID id,
        UUID labTestId,
        String labTestName,
        LabOrderStatus status,
        String result,
        Instant resultAt,
        String performedBy
) {}
