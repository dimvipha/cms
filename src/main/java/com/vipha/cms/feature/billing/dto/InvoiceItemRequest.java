package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.ItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemRequest(
        @NotNull ItemType itemType,
        UUID refId,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Min(1) @Max(10000) Integer quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice
) {}
