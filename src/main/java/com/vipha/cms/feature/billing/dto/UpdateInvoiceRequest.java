package com.vipha.cms.feature.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/** Replaces a draft's tax, discount and full list of lines. */
public record UpdateInvoiceRequest(
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal tax,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discount,
        @NotEmpty List<@Valid InvoiceItemRequest> items
) {}
