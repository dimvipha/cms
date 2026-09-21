package com.vipha.cms.feature.doctor.dto;

import com.vipha.cms.feature.doctor.Specialty;

import java.util.Set;

public record DoctorResponse(
        String userId,
        boolean active,
        Long departmentId,
        Set<Specialty> specialties
) {}
