package com.vipha.cms.feature.patient;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import com.vipha.cms.feature.patient.util.PatientSpecs;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.KeycloakAuthClient;
import com.vipha.cms.security.KeycloakUserService;
import com.vipha.cms.security.Permissions;
import com.vipha.cms.security.Roles;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    private final KeycloakUserService keycloakUserService;
    private final Keycloak keycloak;
    private final KeycloakAuthClient keycloakAuthClient;

    private String generateMrn() {
        return "P-%d-%05d".formatted(Year.now().getValue(), patientRepository.nextMrnSequence());
    }

    @Override
    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        if (patientRepository.existsByPhoneAndActiveTrue(request.phone())) {
            throw new BusinessRuleException("A patient with this phone number already exists");
        }
        Patient patient = patientMapper.toEntity(request);
        patient.setMrn(generateMrn());
        patient.setActive(true);
        if (isSelfRegistration()) {
            String userId = AuthUtils.extractUserId();
            if (patientRepository.findByKeycloakId(userId).isPresent()) {
                throw new BusinessRuleException("Your patient profile already exists");
            }
            patient.setKeycloakId(userId);
        }
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    private boolean isSelfRegistration() {
        Authentication auth = AuthUtils.getAuth();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> Roles.PATIENT.equals(a.getAuthority()));
    }

    @Override
    public PatientResponse findOne(UUID id) {
        Patient patient = getOrThrow(id);
        assertOwnOrAll(patient, Permissions.PATIENT_READ_ALL);
        return patientMapper.toResponse(patient);
    }

    @Override
    public PatientResponse findMine() {
        return patientMapper.toResponse(currentPatient());
    }

    @Override
    public UUID currentPatientId() {
        return currentPatient().getId();
    }

    @Override
    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest request) {
        Patient patient = getOrThrow(id);
        assertOwnOrAll(patient, Permissions.PATIENT_UPDATE_ALL);
        patientMapper.updateEntity(request, patient);
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Patient patient = getOrThrow(id);
        patient.setActive(false);
    }

    @Override
    public Page<PatientResponse> findAll(String search, Pageable pageable) {
        Specification<Patient> spec = PatientSpecs.active();
        Specification<Patient> matching = PatientSpecs.search(search);
        if (matching != null) {
            spec = spec.and(matching);
        }
        return patientRepository.findAll(spec, pageable).map(patientMapper::toResponse);
    }

    private Patient getOrThrow(UUID id) {
        return patientRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Patient not found: " + id));
    }

    private Patient currentPatient() {
        return patientRepository.findByKeycloakId(AuthUtils.extractUserId()).orElseThrow(() ->
                new ResourceNotFoundException("Patient profile not found — create it first"));
    }

    /** Without the -all permission, only the record linked to your own account. */
    private void assertOwnOrAll(Patient patient, String allPermission) {
        if (AuthUtils.hasAuthority(allPermission)
                || AuthUtils.extractUserId().equals(patient.getKeycloakId())) {
            return;
        }
        throw new AccessDeniedException("You can only access your own patient record");
    }
}