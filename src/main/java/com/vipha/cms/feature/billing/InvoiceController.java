package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.billing.dto.BillableVisitResponse;
import com.vipha.cms.feature.billing.dto.CreateInvoiceRequest;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.billing.dto.RecordPaymentRequest;
import com.vipha.cms.feature.billing.dto.UpdateInvoiceRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    /** Drafts the bill for a completed visit from its consultation, prescription and lab orders. */
    @PostMapping("/from-visit/{visitId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createFromVisit(@PathVariable UUID visitId) {
        return invoiceService.createFromVisit(visitId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.create(request);
    }

    @GetMapping
    public Page<InvoiceSummaryResponse> findAll(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return invoiceService.findAll(patientId, visitId, status, pageNumber, pageSize);
    }

    /** The front-desk queue: completed visits that have not been billed yet. */
    @GetMapping("/billable-visits")
    public Page<BillableVisitResponse> findBillableVisits(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return invoiceService.findBillableVisits(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return invoiceService.get(id);
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateInvoiceRequest request) {
        return invoiceService.update(id, request);
    }

    @PostMapping("/{id}/issue")
    public InvoiceResponse issue(@PathVariable UUID id) {
        return invoiceService.issue(id);
    }

    @PostMapping("/{id}/void")
    public InvoiceResponse voidInvoice(@PathVariable UUID id) {
        return invoiceService.voidInvoice(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse recordPayment(@PathVariable UUID id, @Valid @RequestBody RecordPaymentRequest request) {
        return paymentService.record(id, request);
    }

    /** Printable invoice (unpaid) or receipt (paid or part-paid). */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        return invoiceService.renderPdf(id).toInlineResponse();
    }
}
