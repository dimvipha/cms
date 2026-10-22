package com.vipha.cms.feature.billing;

import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.feature.billing.dto.BillableVisitResponse;
import com.vipha.cms.feature.billing.dto.CreateInvoiceRequest;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.billing.dto.UpdateInvoiceRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface InvoiceService {

    /** Drafts an invoice for a completed visit: consultation, prescribed medicines and lab tests. */
    InvoiceResponse createFromVisit(UUID visitId);

    InvoiceResponse create(CreateInvoiceRequest request);

    InvoiceResponse get(UUID id);

    Page<InvoiceSummaryResponse> findAll(UUID patientId, UUID visitId, InvoiceStatus status,
                                         int pageNumber, int pageSize);

    /** One patient's invoices without drafts, for the patient's own view. */
    Page<InvoiceSummaryResponse> findIssued(UUID patientId, int pageNumber, int pageSize);

    Page<BillableVisitResponse> findBillableVisits(int pageNumber, int pageSize);

    InvoiceResponse update(UUID id, UpdateInvoiceRequest request);

    InvoiceResponse issue(UUID id);

    InvoiceResponse voidInvoice(UUID id);

    PdfFile renderPdf(UUID id);
}
