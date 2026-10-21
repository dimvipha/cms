package com.vipha.cms.feature.appointment.dto;

import com.vipha.cms.feature.appointment.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String doctorId,
        String doctorName,
        Long departmentId,
        String departmentName,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String reason,
        AppointmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
