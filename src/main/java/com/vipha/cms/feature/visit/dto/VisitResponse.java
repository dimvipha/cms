package com.vipha.cms.feature.visit.dto;

import com.vipha.cms.feature.patient.dto.PatientSummaryResponse;
import com.vipha.cms.feature.visit.VisitStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        PatientSummaryResponse patient,
        String doctorId,
        String doctorName,
        UUID appointmentId,
        Instant visitDate,
        String chiefComplaint,
        String diagnosisCode,
        String diagnosis,
        String notes,
        VisitStatus status,
        List<VitalSignResponse> vitalSigns,
        Instant createdAt,
        Instant updatedAt
) {}
