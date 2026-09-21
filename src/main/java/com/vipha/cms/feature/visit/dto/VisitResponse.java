package com.vipha.cms.feature.visit.dto;

import com.vipha.cms.feature.visit.VisitStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID patientId,
        String doctorId,
        UUID appointmentId,
        Instant visitDate,
        String chiefComplaint,
        String diagnosis,
        String notes,
        VisitStatus status,
        List<VitalSignResponse> vitalSigns,
        Instant createdAt,
        Instant updatedAt
) {}
