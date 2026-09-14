package com.vipha.cms.feature.patient;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    private String generateMrn() {
        return "P-%d-%05d".formatted(Year.now().getValue(), patientRepository.nextMrnSequence());
    }

    @Override
    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        if (patientRepository.existsByPhone(request.phone())) {
            throw new BusinessRuleException("A patient with this phone number already exists");
        }
        Patient patient = patientMapper.toEntity(request);
        patient.setMrn(generateMrn());
        patient.setActive(true);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse findOne(UUID id) {
        return patientMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest request) {
        Patient patient = getOrThrow(id);
        patientMapper.updateEntity(request, patient);
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Patient patient = getOrThrow(id);
        patient.setActive(false);
    }

    private Patient getOrThrow(UUID id) {
        return patientRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Patient not found: " + id));
    }
}