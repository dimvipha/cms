package com.vipha.cms.feature.visit.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Either start from an appointment (patient and doctor are taken from it) or
 * register a walk-in by giving patientId. doctorId defaults to the caller.
 */
public record CreateVisitRequest(
        UUID appointmentId,
        UUID patientId,
        String doctorId,
        @Size(max = 1000) String chiefComplaint
) {}
