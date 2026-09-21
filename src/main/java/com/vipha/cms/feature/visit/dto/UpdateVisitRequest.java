package com.vipha.cms.feature.visit.dto;

import com.vipha.cms.feature.visit.VisitStatus;
import jakarta.validation.constraints.Size;

public record UpdateVisitRequest(
        @Size(max = 1000) String chiefComplaint,
        String diagnosis,
        String notes,
        VisitStatus status
) {}
