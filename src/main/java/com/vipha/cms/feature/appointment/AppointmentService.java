package com.vipha.cms.feature.appointment;

import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Without appointment:read-all / appointment:write-all, callers reach only their
 * own appointments: patients as the patient, doctors as the doctor.
 */
public interface AppointmentService {

    AppointmentResponse create(CreateAppointmentRequest request);

    AppointmentResponse get(UUID id);

    Page<AppointmentResponse> findAll(UUID patientId, String doctorId, AppointmentStatus status, LocalDate date,
                                      int pageNumber, int pageSize);

    /** The caller's own appointments, newest first: as the doctor for doctors, otherwise as the patient. */
    List<AppointmentResponse> findMine(AppointmentStatus status, LocalDate date);

    /** Reschedule or change the reason, while still SCHEDULED or CONFIRMED. */
    AppointmentResponse update(UUID id, UpdateAppointmentRequest request);

    AppointmentResponse cancel(UUID id);

    /** The patient has arrived; only on the appointment day. */
    AppointmentResponse checkIn(UUID id);

    /** The patient never came; only once the appointment time has passed. */
    AppointmentResponse markNoShow(UUID id);
}
