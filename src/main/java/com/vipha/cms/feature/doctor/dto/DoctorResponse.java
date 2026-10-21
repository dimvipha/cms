package com.vipha.cms.feature.doctor.dto;

import com.vipha.cms.feature.doctor.Specialty;

import java.util.Set;

public record DoctorResponse(
        String userId,
        String firstName,
        String lastName,
        boolean active,
        Long departmentId,
        String departmentName,
        Set<Specialty> specialties
) {}
