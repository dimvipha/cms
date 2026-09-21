package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID patientId,
        UUID visitId,
        Instant issuedAt,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balance,
        InvoiceStatus status,
        List<InvoiceItemResponse> items,
        List<PaymentResponse> payments,
        Instant createdAt,
        Instant updatedAt
) {}
