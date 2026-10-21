package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        PaymentMethod method,
        Instant paidAt,
        String reference,
        String receivedBy,
        String receivedByName
) {}
