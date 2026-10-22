package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientSummaryResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "age",
            expression = "java(java.time.Period.between(patient.getDob(), java.time.LocalDate.now()).getYears())")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "age",
            expression = "java(java.time.Period.between(patient.getDob(), java.time.LocalDate.now()).getYears())")
    PatientSummaryResponse toSummary(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mrn", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Patient toEntity(PatientCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mrn", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(PatientUpdateRequest request, @MappingTarget Patient patient);
}
