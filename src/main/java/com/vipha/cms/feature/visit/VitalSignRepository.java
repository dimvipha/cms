package com.vipha.cms.feature.visit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VitalSignRepository extends JpaRepository<VitalSign, UUID> {
}
