package com.vipha.cms.feature.appointment;

import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.create(request);
    }

    /** Front desk sees every appointment; patients and doctors only their own. */
    @GetMapping
    public Page<AppointmentResponse> findAll(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return appointmentService.findAll(patientId, doctorId, status, date, pageNumber, pageSize);
    }

    /** The caller's own appointments, e.g. {@code ?date=2026-10-26} for a doctor's day. */
    @GetMapping("/me")
    public List<AppointmentResponse> findMine(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.findMine(status, date);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID id) {
        return appointmentService.get(id);
    }

    @PatchMapping("/{id}")
    public AppointmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentRequest request) {
        return appointmentService.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id) {
        return appointmentService.cancel(id);
    }

    @PostMapping("/{id}/check-in")
    public AppointmentResponse checkIn(@PathVariable UUID id) {
        return appointmentService.checkIn(id);
    }

    @PostMapping("/{id}/no-show")
    public AppointmentResponse markNoShow(@PathVariable UUID id) {
        return appointmentService.markNoShow(id);
    }
}
