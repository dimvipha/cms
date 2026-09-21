package com.vipha.cms.feature.appointment.dto;

import com.vipha.cms.feature.appointment.AppointmentStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateAppointmentRequest(
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        @Size(max = 500) String reason,
        AppointmentStatus status
) {}
