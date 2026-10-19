package com.vipha.cms.feature.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, UUID>, JpaSpecificationExecutor<Prescription> {

    boolean existsByVisitId(UUID visitId);

    Optional<Prescription> findByVisitId(UUID visitId);
}
