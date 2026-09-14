package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "age",
            expression = "java(java.time.Period.between(patient.getDob(), java.time.LocalDate.now()).getYears())")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mrn", ignore = true)
    @Mapping(target = "active", ignore = true)
    Patient toEntity(PatientCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PatientUpdateRequest request, @MappingTarget Patient patient);
}
