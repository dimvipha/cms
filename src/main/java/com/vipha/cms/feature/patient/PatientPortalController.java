package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.billing.InvoiceService;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.prescription.PrescriptionService;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.feature.visit.VisitService;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A patient's own medical history: visits, prescriptions and bills. Every call
 * is scoped to the patient record linked to the caller's account, so no
 * visit:read, prescription:read or invoice:read permission is needed.
 */
@RestController
@RequestMapping("/api/v1/patients/me")
@RequiredArgsConstructor
public class PatientPortalController {

    private final PatientService patientService;
    private final VisitService visitService;
    private final PrescriptionService prescriptionService;
    private final InvoiceService invoiceService;

    @GetMapping("/visits")
    public Page<VisitSummaryResponse> visits(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return visitService.findAll(patientService.currentPatientId(), null, null, null, pageNumber, pageSize);
    }

    @GetMapping("/prescriptions")
    public Page<PrescriptionResponse> prescriptions(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return prescriptionService.findAll(patientService.currentPatientId(), null, pageNumber, pageSize);
    }

    @GetMapping(value = "/prescriptions/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> prescriptionPdf(@PathVariable UUID id) {
        if (!prescriptionService.get(id).patientId().equals(patientService.currentPatientId())) {
            throw new AccessDeniedException("This prescription is not yours");
        }
        return prescriptionService.renderPdf(id).toInlineResponse();
    }

    @GetMapping("/invoices")
    public Page<InvoiceSummaryResponse> invoices(
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return invoiceService.findIssued(patientService.currentPatientId(), pageNumber, pageSize);
    }

    @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoicePdf(@PathVariable UUID id) {
        if (!invoiceService.get(id).patient().id().equals(patientService.currentPatientId())) {
            throw new AccessDeniedException("This invoice is not yours");
        }
        return invoiceService.renderPdf(id).toInlineResponse();
    }
}
