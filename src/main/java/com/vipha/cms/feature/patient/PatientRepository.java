package com.vipha.cms.feature.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID>{

    @Query(value = "SELECT nextval('patient_mrn_seq')", nativeQuery = true)
    long nextMrnSequence();

    Optional<Patient> findByKeycloakId(String keycloakId);

    boolean existsByPhone(String phone);
}