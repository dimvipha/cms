package com.vipha.cms.feature.visit;

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

    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(visit.getDoctor().getUserId()))")
    @Mapping(target = "appointmentId", source = "appointment.id")
    public abstract VisitResponse toResponse(Visit visit);

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
