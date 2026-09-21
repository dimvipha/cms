package com.vipha.cms.feature.visit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateVisitRequest(
        @NotNull UUID patientId,
        @NotNull String doctorId,
        UUID appointmentId,
        @Size(max = 1000) String chiefComplaint
) {}
