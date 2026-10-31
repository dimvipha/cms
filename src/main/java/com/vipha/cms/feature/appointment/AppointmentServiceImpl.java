package com.vipha.cms.feature.appointment;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import com.vipha.cms.feature.appointment.util.AppointmentSpecs;
import com.vipha.cms.feature.care.CareScope;
import com.vipha.cms.feature.department.DepartmentService;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.Permissions;
import com.vipha.cms.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    /** Booked and not yet acted on: the only states that can be rescheduled, checked in or marked no-show. */
    private static final Set<AppointmentStatus> OPEN_STATUSES =
            EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "appointmentDate", "appointmentTime");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentService departmentService;
    private final AvailabilityService availabilityService;
    private final AppointmentMapper appointmentMapper;
    private final ClinicProperties clinicProperties;
    private final CareScope careScope;

    @Override
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Patient patient = patientToBookFor(request.patientId());
        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient account is inactive");
        }

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("Doctor is not available");
        }
        assertFreeSlot(doctor, request.appointmentDate(), request.appointmentTime());

        boolean patientBooked = appointmentRepository
                .existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        patient.getId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentStatus.CANCELLED);
        if (patientBooked) {
            throw new BusinessRuleException("The patient already has an appointment at this time");
        }

        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setDepartment(request.departmentId() != null
                ? departmentService.getEntity(request.departmentId())
                : doctor.getDepartment());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse get(UUID id) {
        Appointment appointment = getAppointment(id);
        assertCanRead(appointment);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> findAll(UUID patientId, String doctorId, AppointmentStatus status,
                                             LocalDate date, int pageNumber, int pageSize) {
        if (!readsAll()) {
            OwnSide own = ownSide();
            if (own.patientId() != null) {
                patientId = own.patientId();
            } else {
                doctorId = own.doctorId();
            }
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize, NEWEST_FIRST);
        return appointmentRepository.findAll(spec(patientId, doctorId, status, date), pageable)
                .map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> findMine(AppointmentStatus status, LocalDate date) {
        OwnSide own = ownSide();
        return appointmentRepository.findAll(spec(own.patientId(), own.doctorId(), status, date), NEWEST_FIRST)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Override
    public AppointmentResponse update(UUID id, UpdateAppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        assertCanManage(appointment);
        if (!OPEN_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessRuleException(
                    "A " + appointment.getStatus() + " appointment can no longer be changed");
        }

        LocalDate date = request.appointmentDate() != null ? request.appointmentDate() : appointment.getAppointmentDate();
        LocalTime time = request.appointmentTime() != null ? request.appointmentTime() : appointment.getAppointmentTime();
        if (!date.equals(appointment.getAppointmentDate()) || !time.equals(appointment.getAppointmentTime())) {
            assertFreeSlot(appointment.getDoctor(), date, time);
        }

        appointmentMapper.updateEntity(request, appointment);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public AppointmentResponse cancel(UUID id) {
        Appointment appointment = getAppointment(id);
        assertCanManage(appointment);
        switch (appointment.getStatus()) {
            case COMPLETED -> throw new BusinessRuleException("Completed appointment cannot be cancelled");
            case CANCELLED -> throw new BusinessRuleException("Appointment is already cancelled");
            case IN_PROGRESS -> throw new BusinessRuleException("The visit has started; cancel the visit instead");
            case NO_SHOW -> throw new BusinessRuleException("A no-show appointment cannot be cancelled");
            default -> appointment.setStatus(AppointmentStatus.CANCELLED);
        }
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public AppointmentResponse checkIn(UUID id) {
        Appointment appointment = getAppointment(id);
        if (!OPEN_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessRuleException("Cannot check in an appointment that is " + appointment.getStatus());
        }
        LocalDate today = LocalDate.now(clinicProperties.timeZone());
        if (!appointment.getAppointmentDate().equals(today)) {
            throw new BusinessRuleException(
                    "Check-in is only possible on the appointment day, " + appointment.getAppointmentDate());
        }
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public AppointmentResponse markNoShow(UUID id) {
        Appointment appointment = getAppointment(id);
        if (!OPEN_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot mark a no-show for an appointment that is " + appointment.getStatus());
        }
        LocalDateTime start = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
        if (start.isAfter(LocalDateTime.now(clinicProperties.timeZone()))) {
            throw new BusinessRuleException("The appointment has not started yet; it is at " + start);
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return appointmentMapper.toResponse(appointment);
    }

    /** Patients book for themselves; front-desk staff for the patient they name. */
    private Patient patientToBookFor(UUID patientId) {
        if (AuthUtils.hasAuthority(Permissions.APPOINTMENT_WRITE_ALL)) {
            if (patientId == null) {
                throw new BusinessRuleException("patientId is required when booking for a patient");
            }
            return patientRepository.findById(patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        }
        Patient me = currentPatient();
        if (patientId != null && !patientId.equals(me.getId())) {
            throw new AccessDeniedException("You can only book appointments for yourself");
        }
        return me;
    }

    /** The time must be one of the doctor's free slots: inside their schedule, not taken, not past. */
    private void assertFreeSlot(Doctor doctor, LocalDate date, LocalTime time) {
        if (!availabilityService.getAvailableSlots(doctor.getUserId(), date).contains(time)) {
            throw new BusinessRuleException(
                    "%s at %s is not a free slot for this doctor; pick one from their availability".formatted(date, time));
        }
    }

    private record OwnSide(UUID patientId, String doctorId) {}

    private OwnSide ownSide() {
        if (AuthUtils.hasAuthority(Roles.DOCTOR)) {
            return new OwnSide(null, AuthUtils.extractUserId());
        }
        return new OwnSide(currentPatient().getId(), null);
    }

    /** A doctor reads their own, even with another role that reads every appointment. */
    private boolean readsAll() {
        return AuthUtils.hasAuthority(Permissions.APPOINTMENT_READ_ALL) && careScope.limitedToDoctor().isEmpty();
    }

    private void assertCanRead(Appointment appointment) {
        if (readsAll()) {
            return;
        }
        String userId = AuthUtils.extractUserId();
        if (userId.equals(appointment.getDoctor().getUserId())
                || userId.equals(appointment.getPatient().getKeycloakId())) {
            return;
        }
        throw new AccessDeniedException("You cannot access this appointment");
    }

    private void assertCanManage(Appointment appointment) {
        if (AuthUtils.hasAuthority(Permissions.APPOINTMENT_WRITE_ALL)
                || AuthUtils.extractUserId().equals(appointment.getPatient().getKeycloakId())) {
            return;
        }
        throw new AccessDeniedException("You cannot change this appointment");
    }

    private Specification<Appointment> spec(UUID patientId, String doctorId, AppointmentStatus status, LocalDate date) {
        return Specification.allOf(
                AppointmentSpecs.patient(patientId),
                AppointmentSpecs.doctor(doctorId),
                AppointmentSpecs.status(status),
                AppointmentSpecs.onDate(date));
    }

    private Appointment getAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private Patient currentPatient() {
        String userId = AuthUtils.extractUserId();
        return patientRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found — complete your profile first"));
    }
}
