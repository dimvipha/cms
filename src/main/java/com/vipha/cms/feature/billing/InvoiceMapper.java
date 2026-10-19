package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.billing.dto.BillableVisitResponse;
import com.vipha.cms.feature.billing.dto.InvoiceItemResponse;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.billing.dto.PaymentResponse;
import com.vipha.cms.feature.patient.PatientMapper;
import com.vipha.cms.feature.visit.Visit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PatientMapper.class)
public interface InvoiceMapper {

    @Mapping(target = "visitId", source = "visit.id")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "visitId", source = "visit.id")
    InvoiceSummaryResponse toSummary(Invoice invoice);

    InvoiceItemResponse toItemResponse(InvoiceItem item);

    @Mapping(target = "invoiceId", source = "invoice.id")
    PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "doctorId", source = "doctor.userId")
    BillableVisitResponse toBillableVisit(Visit visit);
}
