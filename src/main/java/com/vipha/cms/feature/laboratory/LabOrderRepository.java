package com.vipha.cms.feature.laboratory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {

    List<LabOrder> findByVisitId(UUID visitId);
}
