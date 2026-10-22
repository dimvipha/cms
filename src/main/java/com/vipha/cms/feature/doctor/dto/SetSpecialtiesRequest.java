package com.vipha.cms.feature.doctor.dto;

import com.vipha.cms.feature.doctor.Specialty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SetSpecialtiesRequest(
        @NotNull Set<Specialty> specialties
) {}
