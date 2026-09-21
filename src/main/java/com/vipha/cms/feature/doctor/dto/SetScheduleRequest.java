package com.vipha.cms.feature.doctor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetScheduleRequest(
        @NotNull
        @Valid
        List<ScheduleEntryDto> entries
) {}
