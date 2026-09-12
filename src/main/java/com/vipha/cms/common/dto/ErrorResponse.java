package com.vipha.cms.common.dto;

import java.time.Instant;

public record ErrorResponse<T>(
        int code,
        boolean success,
        String message,
        Instant timestamp,
        T errorDetail
) {
}