package com.vipha.cms.feature.billing;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.billing.dto.BillableVisitResponse;
import com.vipha.cms.feature.billing.dto.CreateInvoiceRequest;
import com.vipha.cms.feature.billing.dto.InvoiceItemRequest;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.billing.dto.UpdateInvoiceRequest;
import com.vipha.cms.feature.billing.util.InvoiceSpecs;
import com.vipha.cms.feature.laboratory.LabOrder;
import com.vipha.cms.feature.laboratory.LabOrderItem;
import com.vipha.cms.feature.laboratory.LabOrderRepository;
import com.vipha.cms.feature.laboratory.LabOrderStatus;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.prescription.PrescriptionItem;
import com.vipha.cms.feature.prescription.PrescriptionRepository;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitRepository;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.security.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabOrderRepository labOrderRepository;
    private final InvoiceMapper invoiceMapper;
    private final ReceiptPdfRenderer receiptRenderer;
    private final KeycloakUserService keycloakUserService;
    private final BillingProperties billingProperties;
    private final ClinicProperties clinicProperties;

    @Override
    public InvoiceResponse createFromVisit(UUID visitId) {
        Visit visit = getBillableVisit(visitId);
        Invoice invoice = newDraft(visit.getPatient(), visit);

        addLine(invoice, ItemType.CONSULTATION, visit.getId(), "Consultation fee", 1,
                billingProperties.consultationFee());

        // prices are copied onto the lines so later price changes don't rewrite this bill
        prescriptionRepository.findByVisitId(visitId).ifPresent(prescription -> {
            for (PrescriptionItem item : prescription.getItems()) {
                Medicine medicine = item.getMedicine();
                addLine(invoice, ItemType.MEDICINE, item.getId(), medicine.getName(),
                        item.getQuantity(), medicine.getPrice());
            }
        });

        for (LabOrder order : labOrderRepository.findByVisitId(visitId)) {
            if (order.getStatus() == LabOrderStatus.CANCELLED) {
                continue;
            }
            for (LabOrderItem item : order.getItems()) {
                if (item.getStatus() != LabOrderStatus.CANCELLED) {
                    addLine(invoice, ItemType.LAB_TEST, item.getId(), item.getLabTest().getName(),
                            1, item.getLabTest().getPrice());
                }
            }
        }

        invoice.recalculateTotals();
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Visit visit = null;
        if (request.visitId() != null) {
            visit = getBillableVisit(request.visitId());
            if (!visit.getPatient().getId().equals(patient.getId())) {
                throw new BusinessRuleException("Visit belongs to a different patient");
            }
        }

        Invoice invoice = newDraft(patient, visit);
        applyCharges(invoice, request.tax(), request.discount(), request.items());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        return invoiceMapper.toResponse(getInvoice(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> findAll(UUID patientId, UUID visitId, InvoiceStatus status,
                                                int pageNumber, int pageSize) {
        Specification<Invoice> spec = Specification.allOf(
                InvoiceSpecs.patient(patientId),
                InvoiceSpecs.visit(visitId),
                InvoiceSpecs.status(status));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "issuedAt"));
        return invoiceRepository.findAll(spec, pageable).map(invoiceMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BillableVisitResponse> findBillableVisits(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "visitDate"));
        return invoiceRepository.findUnbilledVisits(VisitStatus.COMPLETED, InvoiceStatus.VOID, pageable)
                .map(invoiceMapper::toBillableVisit);
    }

    @Override
    public InvoiceResponse update(UUID id, UpdateInvoiceRequest request) {
        Invoice invoice = getInvoice(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only draft invoices can be edited; this one is " + invoice.getStatus());
        }
        applyCharges(invoice, request.tax(), request.discount(), request.items());
        // assign ids to the new lines before they are mapped
        invoiceRepository.flush();
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse issue(UUID id) {
        Invoice invoice = getInvoice(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Invoice has already been issued");
        }
        invoice.setIssuedAt(Instant.now());
        invoice.refreshPaymentStatus();
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse voidInvoice(UUID id) {
        // locked so a payment can't land between the check and the status change
        Invoice invoice = invoiceRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessRuleException("Invoice is already void");
        }
        if (!invoice.getPayments().isEmpty()) {
            throw new BusinessRuleException("Invoices with payments cannot be voided");
        }
        invoice.setStatus(InvoiceStatus.VOID);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public PdfFile renderPdf(UUID id) {
        Invoice invoice = getInvoice(id);
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Issue the invoice before printing it");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessRuleException("Void invoices cannot be printed");
        }

        Map<String, String> staffNames = new HashMap<>();
        invoice.getPayments().stream()
                .map(Payment::getReceivedBy)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> keycloakUserService.findFullName(userId)
                        .ifPresent(name -> staffNames.put(userId, name)));

        byte[] content = receiptRenderer.render(invoice, staffNames);
        String prefix = invoice.getStatus() == InvoiceStatus.UNPAID ? "invoice" : "receipt";
        return new PdfFile(prefix + "-" + invoice.getInvoiceNumber() + ".pdf", content);
    }

    private Invoice getInvoice(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private Visit getBillableVisit(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        if (visit.getStatus() != VisitStatus.COMPLETED) {
            throw new BusinessRuleException("Only completed visits can be billed; this visit is " + visit.getStatus());
        }
        if (invoiceRepository.existsByVisitIdAndStatusNot(visitId, InvoiceStatus.VOID)) {
            throw new BusinessRuleException("This visit already has an invoice; void it before billing again");
        }
        return visit;
    }

    private Invoice newDraft(Patient patient, Visit visit) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setPatient(patient);
        invoice.setVisit(visit);
        invoice.setIssuedAt(Instant.now()); // replaced when the draft is issued
        invoice.setStatus(InvoiceStatus.DRAFT);
        return invoice;
    }

    private String nextInvoiceNumber() {
        int year = Year.now(clinicProperties.timeZone()).getValue();
        return "INV-%d-%05d".formatted(year, invoiceRepository.nextInvoiceSequence());
    }

    private void applyCharges(Invoice invoice, BigDecimal tax, BigDecimal discount, List<InvoiceItemRequest> items) {
        invoice.getItems().clear();
        for (InvoiceItemRequest item : items) {
            addLine(invoice, item.itemType(), item.refId(), item.description(), item.quantity(), item.unitPrice());
        }
        invoice.setTax(money(tax));
        invoice.setDiscount(money(discount));
        invoice.recalculateTotals();
        if (invoice.getTotal().signum() < 0) {
            throw new BusinessRuleException("Discount cannot be more than the subtotal plus tax");
        }
    }

    private static void addLine(Invoice invoice, ItemType type, UUID refId, String description,
                                int quantity, BigDecimal unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setItemType(type);
        item.setRefId(refId);
        item.setDescription(description);
        item.setQuantity(quantity);
        item.setUnitPrice(money(unitPrice));
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        invoice.getItems().add(item);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
