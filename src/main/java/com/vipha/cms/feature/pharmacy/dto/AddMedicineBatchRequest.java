package com.vipha.cms.feature.pharmacy.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AddMedicineBatchRequest(
        @NotBlank @Size(max = 60) String batchNumber,
        @NotNull @Future LocalDate expiryDate,
        @NotNull @Min(1) Integer stockQuantity,
        @NotNull @PastOrPresent LocalDate receivedAt
) {}
