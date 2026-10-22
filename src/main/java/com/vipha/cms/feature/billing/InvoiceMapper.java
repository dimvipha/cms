package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.billing.dto.BillableVisitResponse;
import com.vipha.cms.feature.billing.dto.InvoiceItemResponse;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.InvoiceSummaryResponse;
import com.vipha.cms.feature.billing.dto.PaymentResponse;
import com.vipha.cms.feature.patient.PatientMapper;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.security.UserNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = PatientMapper.class)
public abstract class InvoiceMapper {

    @Autowired
    protected UserNames userNames;

    @Mapping(target = "visitId", source = "visit.id")
    public abstract InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "visitId", source = "visit.id")
    public abstract InvoiceSummaryResponse toSummary(Invoice invoice);

    public abstract InvoiceItemResponse toItemResponse(InvoiceItem item);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "receivedByName", expression = "java(userNames.fullName(payment.getReceivedBy()))")
    public abstract PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(visit.getDoctor().getUserId()))")
    public abstract BillableVisitResponse toBillableVisit(Visit visit);
}
