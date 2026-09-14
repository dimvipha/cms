package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;

import java.util.UUID;

public interface PatientService {
    PatientResponse create(PatientCreateRequest request);
    PatientResponse findOne(UUID id);
    PatientResponse update(UUID id, PatientUpdateRequest request);
    void delete(UUID id);
}
