package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.patient.dto.PatientSummaryResponse;

import java.time.Instant;
import java.util.UUID;

/** A completed visit waiting for an invoice; carries no clinical detail. */
public record BillableVisitResponse(
        UUID visitId,
        PatientSummaryResponse patient,
        String doctorId,
        Instant visitDate
) {}
