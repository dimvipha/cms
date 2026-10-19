package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.RecordPaymentRequest;

import java.util.UUID;

public interface PaymentService {

    /** Records a payment against an issued invoice and returns the invoice with its new balance. */
    InvoiceResponse record(UUID invoiceId, RecordPaymentRequest request);
}
