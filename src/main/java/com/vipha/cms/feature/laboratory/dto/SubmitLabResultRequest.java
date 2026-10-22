package com.vipha.cms.feature.laboratory.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitLabResultRequest(
        @NotBlank String result,
        String performedBy
) {}
