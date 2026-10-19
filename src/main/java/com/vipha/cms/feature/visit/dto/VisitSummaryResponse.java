package com.vipha.cms.feature.visit.dto;

import com.vipha.cms.feature.patient.dto.PatientSummaryResponse;
import com.vipha.cms.feature.visit.VisitStatus;

import java.time.Instant;
import java.util.UUID;

/** List row for queues and history; vitals are left to the detail view. */
public record VisitSummaryResponse(
        UUID id,
        PatientSummaryResponse patient,
        String doctorId,
        UUID appointmentId,
        Instant visitDate,
        String chiefComplaint,
        String diagnosisCode,
        String diagnosis,
        VisitStatus status
) {}
