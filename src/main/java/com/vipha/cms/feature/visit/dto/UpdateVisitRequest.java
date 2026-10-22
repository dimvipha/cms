package com.vipha.cms.feature.visit.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update of the consultation notes; null fields are left unchanged. */
public record UpdateVisitRequest(
        @Size(max = 1000) String chiefComplaint,
        @Pattern(regexp = "^([A-Za-z][0-9][0-9A-Za-z](\\.[0-9A-Za-z]{1,4})?)?$",
                message = "Diagnosis code must be an ICD-10 code, e.g. J06.9")
        String diagnosisCode,
        String diagnosis,
        String notes
) {}
