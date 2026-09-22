package com.vipha.cms.feature.appointment;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    public void createAppointment(CreateAppointmentRequest request) {
        Patient patient = currentPatient();

        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient account is inactive");
        }

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.isActive()) {
            throw new BusinessRuleException("Doctor is not available");
        }

        boolean doctorBooked = appointmentRepository
                .existsByDoctorUserIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        doctor.getUserId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentStatus.CANCELLED);

        if (doctorBooked) {
            throw new BusinessRuleException("Appointment slot is already booked");
        }

        boolean patientBooked = appointmentRepository
                .existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        patient.getId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentStatus.CANCELLED);

        if (patientBooked) {
            throw new BusinessRuleException("You already have an appointment at this time");
        }

        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID appointmentId) {
        Patient patient = currentPatient();
        Appointment appointment = getOwnedAppointment(appointmentId, patient, "You cannot access this appointment");
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public AppointmentResponse updateAppointment(UUID appointmentId, UpdateAppointmentRequest request) {
        Patient patient = currentPatient();
        Appointment appointment = getOwnedAppointment(appointmentId, patient, "You cannot update this appointment");

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Appointment cannot be updated");
        }

        boolean slotBooked = appointmentRepository
                .existsByDoctorUserIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                        appointment.getDoctor().getUserId(),
                        request.appointmentDate() != null ? request.appointmentDate() : appointment.getAppointmentDate(),
                        request.appointmentTime() != null ? request.appointmentTime() : appointment.getAppointmentTime(),
                        AppointmentStatus.CANCELLED,
                        appointmentId);

        if (slotBooked) {
            throw new BusinessRuleException("Appointment slot is already booked");
        }

        appointmentMapper.updateEntity(request, appointment);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public void cancelAppointment(UUID appointmentId) {
        Patient patient = currentPatient();
        Appointment appointment = getOwnedAppointment(appointmentId, patient, "You cannot cancel this appointment");

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Completed appointment cannot be cancelled");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Appointment is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getMyAppointments(int pageNumber, int pageSize) {
        Sort sort = Sort.by(Sort.Direction.DESC, "appointmentDate", "appointmentTime");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return appointmentRepository.findAll(pageable).map(appointmentMapper::toResponse);

    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointmentsList() {
        Patient patient = currentPatient();
        return appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patient.getId())
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    private Patient currentPatient() {
        String userId = AuthUtils.extractUserId();
        return patientRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found — complete your profile first"));
    }

    private Appointment getOwnedAppointment(UUID appointmentId, Patient patient, String forbiddenMessage) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new BusinessRuleException(forbiddenMessage);
        }
        return appointment;
    }
}
