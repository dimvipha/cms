package com.vipha.cms.feature.visit;

import com.vipha.cms.feature.billing.InvoiceRepository;
import com.vipha.cms.feature.billing.InvoiceStatus;
import com.vipha.cms.feature.patient.PatientMapper;
import com.vipha.cms.feature.visit.dto.RecordVitalSignRequest;
import com.vipha.cms.feature.visit.dto.UpdateVisitRequest;
import com.vipha.cms.feature.visit.dto.VisitResponse;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import com.vipha.cms.feature.visit.dto.VitalSignResponse;
import com.vipha.cms.security.UserNames;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = PatientMapper.class)
public abstract class VisitMapper {

    @Autowired
    protected UserNames userNames;

    @Autowired
    protected InvoiceRepository invoiceRepository;

    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(visit.getDoctor().getUserId()))")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "billed", expression = "java(isBilled(visit))")
    public abstract VisitResponse toResponse(Visit visit);

    /** Only completed visits can be billed, and a voided invoice no longer counts. */
    protected boolean isBilled(Visit visit) {
        return visit.getStatus() == VisitStatus.COMPLETED
                && invoiceRepository.existsByVisitIdAndStatusNot(visit.getId(), InvoiceStatus.VOID);
    }

    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(visit.getDoctor().getUserId()))")
    @Mapping(target = "appointmentId", source = "appointment.id")
    public abstract VisitSummaryResponse toSummary(Visit visit);

    @Mapping(target = "visitId", source = "visit.id")
    @Mapping(target = "recordedBy", source = "createdBy")
    @Mapping(target = "recordedByName", expression = "java(userNames.fullName(vitalSign.getCreatedBy()))")
    public abstract VitalSignResponse toVitalSignResponse(VitalSign vitalSign);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "visit", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    public abstract VitalSign toVitalSign(RecordVitalSignRequest request);

    /** Replaces every measurement, so one left out is cleared. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "visit", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    public abstract void updateVitalSign(RecordVitalSignRequest request, @MappingTarget VitalSign vitalSign);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "visitDate", ignore = true)
    @Mapping(target = "diagnosisCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "vitalSigns", ignore = true)
    public abstract void updateEntity(UpdateVisitRequest request, @MappingTarget Visit visit);
}
