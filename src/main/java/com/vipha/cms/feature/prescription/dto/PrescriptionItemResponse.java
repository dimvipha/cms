package com.vipha.cms.feature.prescription.dto;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID id,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        String dosage,
        String frequency,
        String duration,
        Integer quantity,
        String instructions
) {}
