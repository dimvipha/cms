package com.vipha.cms.feature.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID>, JpaSpecificationExecutor<Visit> {

    boolean existsByAppointmentId(UUID appointmentId);

    @Override
    @EntityGraph(attributePaths = "patient")
    Page<Visit> findAll(Specification<Visit> spec, Pageable pageable);
}
