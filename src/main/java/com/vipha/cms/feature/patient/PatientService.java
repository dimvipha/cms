package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PatientService {
    PatientResponse create(PatientCreateRequest request);
    PatientResponse findOne(UUID id);
    /** The record linked to the caller's account. */
    PatientResponse findMine();
    UUID currentPatientId();
    PatientResponse update(UUID id, PatientUpdateRequest request);
    void delete(UUID id);

    /** {@code search} matches first or last name, MRN or phone; null lists everyone. */
    Page<PatientResponse> findAll(String search, Pageable pageable);
}
