package com.vipha.cms.feature.laboratory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateLabOrderRequest(
        @NotNull UUID visitId,
        String notes,
        @NotEmpty List<UUID> labTestIds
) {}
