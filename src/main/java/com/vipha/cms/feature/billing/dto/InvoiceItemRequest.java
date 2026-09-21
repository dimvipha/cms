package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemRequest(
        @NotNull ItemType itemType,
        UUID refId,
        @NotBlank String description,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
