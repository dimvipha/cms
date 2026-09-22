package com.vipha.cms.feature.appointment;

import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    void createAppointment(CreateAppointmentRequest request);

    AppointmentResponse getAppointmentById(UUID appointmentId);

    AppointmentResponse updateAppointment(UUID appointmentId, UpdateAppointmentRequest request);

    void cancelAppointment(UUID appointmentId);

    Page<AppointmentResponse> getMyAppointments(int page, int size);
    List<AppointmentResponse> getMyAppointmentsList();
}
