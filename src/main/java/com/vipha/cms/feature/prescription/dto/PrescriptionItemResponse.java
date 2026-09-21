package com.vipha.cms.feature.prescription.dto;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID id,
        UUID medicineId,
        String medicineName,
        String dosage,
        String frequency,
        String duration,
        Integer quantity,
        String instructions
) {}
