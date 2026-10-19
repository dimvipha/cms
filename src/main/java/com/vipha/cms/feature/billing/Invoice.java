package com.vipha.cms.feature.billing;

import com.vipha.cms.common.BaseEntity;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.visit.Visit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@Setter
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @BatchSize(size = 50)
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("paidAt ASC")
    @BatchSize(size = 50)
    private List<Payment> payments = new ArrayList<>();

    public BigDecimal getBalance() {
        return total.subtract(amountPaid);
    }

    /** Subtotal is the sum of the lines; total adds tax and takes off the discount. */
    public void recalculateTotals() {
        subtotal = items.stream()
                .map(InvoiceItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(tax).subtract(discount);
    }

    /**
     * Once issued, the status follows from what has been paid; it is never set by hand.
     * A zero-total invoice counts as paid.
     */
    public void refreshPaymentStatus() {
        amountPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amountPaid.compareTo(total) >= 0) {
            status = InvoiceStatus.PAID;
        } else if (amountPaid.signum() == 0) {
            status = InvoiceStatus.UNPAID;
        } else {
            status = InvoiceStatus.PARTIALLY_PAID;
        }
    }
}
