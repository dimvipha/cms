package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.InvoiceStatus;
import com.vipha.cms.feature.patient.dto.PatientSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** List row; lines and payments are left to the detail view. */
public record InvoiceSummaryResponse(
        UUID id,
        String invoiceNumber,
        PatientSummaryResponse patient,
        UUID visitId,
        Instant issuedAt,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balance,
        InvoiceStatus status
) {}
