package com.vipha.cms.feature.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Replaces the notes and the full list of items. */
public record UpdatePrescriptionRequest(
        String notes,
        @NotEmpty List<@Valid PrescriptionItemRequest> items
) {}
