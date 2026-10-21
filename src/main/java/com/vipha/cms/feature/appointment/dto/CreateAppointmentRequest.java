package com.vipha.cms.feature.appointment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Patients book for themselves and leave {@code patientId} out; front-desk staff
 * (appointment:write-all) must name the patient. {@code departmentId} defaults
 * to the doctor's department, and the time must be one of the doctor's free slots.
 */
public record CreateAppointmentRequest(
        UUID patientId,
        @NotBlank String doctorId,
        Long departmentId,
        @NotNull @FutureOrPresent LocalDate appointmentDate,
        @NotNull LocalTime appointmentTime,
        @Size(max = 500) String reason
) {}
