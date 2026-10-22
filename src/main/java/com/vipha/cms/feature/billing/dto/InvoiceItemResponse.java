package com.vipha.cms.feature.billing.dto;

import com.vipha.cms.feature.billing.ItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        ItemType itemType,
        UUID refId,
        String description,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
