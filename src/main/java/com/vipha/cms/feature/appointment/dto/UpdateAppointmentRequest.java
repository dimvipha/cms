package com.vipha.cms.feature.appointment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateAppointmentRequest(
        @FutureOrPresent LocalDate appointmentDate,
        LocalTime appointmentTime,
        @Size(max = 500) String reason
) {}
