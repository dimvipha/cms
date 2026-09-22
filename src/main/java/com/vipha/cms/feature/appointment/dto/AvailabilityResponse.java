package com.vipha.cms.feature.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(
        String doctorId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") List<LocalTime> availableSlots
) {}
