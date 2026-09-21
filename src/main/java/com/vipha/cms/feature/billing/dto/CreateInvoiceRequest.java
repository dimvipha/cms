package com.vipha.cms.feature.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID patientId,
        UUID visitId,
        @DecimalMin("0.00") BigDecimal tax,
        @DecimalMin("0.00") BigDecimal discount,
        @NotEmpty @Valid List<InvoiceItemRequest> items
) {}
