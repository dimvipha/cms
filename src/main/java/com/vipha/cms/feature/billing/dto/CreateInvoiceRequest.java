package com.vipha.cms.feature.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A hand-built invoice, e.g. a procedure or an over-the-counter sale. Starts as a draft. */
public record CreateInvoiceRequest(
        @NotNull UUID patientId,
        UUID visitId,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tax,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discount,
        @NotEmpty List<@Valid InvoiceItemRequest> items
) {}
