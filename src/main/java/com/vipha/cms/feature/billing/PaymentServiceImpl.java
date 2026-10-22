package com.vipha.cms.feature.billing;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.RecordPaymentRequest;
import com.vipha.cms.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    public InvoiceResponse record(UUID invoiceId, RecordPaymentRequest request) {
        // locked until commit so two cashiers can't both pay the same balance
        Invoice invoice = invoiceRepository.findForUpdateById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        switch (invoice.getStatus()) {
            case DRAFT -> throw new BusinessRuleException("Issue the invoice before taking payment");
            case PAID -> throw new BusinessRuleException("Invoice is already paid in full");
            case VOID -> throw new BusinessRuleException("Invoice is void");
            case UNPAID, PARTIALLY_PAID -> { }
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(invoice.getBalance()) > 0) {
            throw new BusinessRuleException(
                    "Payment of " + amount + " is more than the balance of " + invoice.getBalance());
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMethod(request.method());
        payment.setReference(request.reference());
        payment.setPaidAt(Instant.now());
        payment.setReceivedBy(AuthUtils.extractUserId());

        // load existing payments first so the new one is added to a complete list
        Hibernate.initialize(invoice.getPayments());
        invoice.getPayments().add(paymentRepository.save(payment));
        invoice.refreshPaymentStatus();
        return invoiceMapper.toResponse(invoice);
    }
}
