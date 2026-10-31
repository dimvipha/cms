package com.vipha.cms.feature.visit;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.appointment.Appointment;
import com.vipha.cms.feature.appointment.AppointmentRepository;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import com.vipha.cms.feature.care.CareScope;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.feature.visit.dto.CreateVisitRequest;
import com.vipha.cms.feature.visit.dto.RecordVitalSignRequest;
import com.vipha.cms.feature.visit.dto.UpdateVisitRequest;
import com.vipha.cms.feature.visit.dto.VisitResponse;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import com.vipha.cms.feature.visit.dto.VitalSignResponse;
import com.vipha.cms.feature.visit.util.VisitSpecs;
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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitServiceImpl implements VisitService {

    private static final Set<AppointmentStatus> STARTABLE_APPOINTMENT_STATUSES = EnumSet.of(
            AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    private final VisitRepository visitRepository;
    private final VitalSignRepository vitalSignRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final VisitMapper visitMapper;
    private final ClinicProperties clinicProperties;
    private final CareScope careScope;

    @Override
    public VisitResponse create(CreateVisitRequest request) {
        Visit visit = new Visit();
        if (request.appointmentId() != null) {
            fillFromAppointment(visit, request);
        } else {
            fillWalkIn(visit, request);
        }

        if (!visit.getDoctor().isActive()) {
            throw new BusinessRuleException("Doctor is not available");
        }
        if (request.chiefComplaint() != null) {
            visit.setChiefComplaint(request.chiefComplaint());
        }
        visit.setVisitDate(Instant.now());
        visit.setStatus(VisitStatus.OPEN);
        return visitMapper.toResponse(visitRepository.save(visit));
    }

    @Override
    @Transactional(readOnly = true)
    public VisitResponse get(UUID id) {
        Visit visit = getVisit(id);
        careScope.assertCovers(visit.getPatient().getId());
        return visitMapper.toResponse(visit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitSummaryResponse> findAll(UUID patientId, String doctorId, VisitStatus status, LocalDate date,
                                              int pageNumber, int pageSize) {
        Specification<Visit> spec = Specification.allOf(
                VisitSpecs.patient(patientId),
                VisitSpecs.doctor(doctorId),
                VisitSpecs.status(status),
                VisitSpecs.onDate(date, clinicProperties.timeZone()),
                // a doctor sees their own patients' visits, whoever the doctor was
                careScope.narrow(root -> root.get("patient").get("id")));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "visitDate"));
        return visitRepository.findAll(spec, pageable).map(visitMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VisitSummaryResponse> findMine(VisitStatus status, LocalDate date, int pageNumber, int pageSize) {
        return findAll(null, AuthUtils.extractUserId(), status, date, pageNumber, pageSize);
    }

    @Override
    public VisitResponse update(UUID id, UpdateVisitRequest request) {
        Visit visit = getEditableEntity(id);
        if (visit.getStatus() == VisitStatus.COMPLETED && request.diagnosis() != null
                && !StringUtils.hasText(request.diagnosis())) {
            throw new BusinessRuleException("A completed visit must keep its diagnosis");
        }
        visitMapper.updateEntity(request, visit);
        if (request.diagnosisCode() != null) {
            visit.setDiagnosisCode(StringUtils.hasText(request.diagnosisCode())
                    ? request.diagnosisCode().toUpperCase(Locale.ROOT)
                    : null);
        }
        return visitMapper.toResponse(visit);
    }

    @Override
    public VisitResponse start(UUID id) {
        Visit visit = getVisit(id);
        assertAssignedDoctor(visit);
        moveTo(visit, VisitStatus.IN_PROGRESS);
        return visitMapper.toResponse(visit);
    }

    @Override
    public VisitResponse complete(UUID id) {
        Visit visit = getVisit(id);
        assertAssignedDoctor(visit);
        assertCanMove(visit, VisitStatus.COMPLETED);
        if (!StringUtils.hasText(visit.getDiagnosis())) {
            throw new BusinessRuleException("Record a diagnosis before completing the visit");
        }
        moveTo(visit, VisitStatus.COMPLETED);
        if (visit.getAppointment() != null) {
            visit.getAppointment().setStatus(AppointmentStatus.COMPLETED);
        }
        return visitMapper.toResponse(visit);
    }

    @Override
    public VisitResponse cancel(UUID id) {
        Visit visit = getVisit(id);
        assertAssignedDoctor(visit);
        moveTo(visit, VisitStatus.CANCELLED);
        if (visit.getAppointment() != null) {
            visit.getAppointment().setStatus(AppointmentStatus.CANCELLED);
        }
        return visitMapper.toResponse(visit);
    }

    @Override
    public VitalSignResponse recordVitals(UUID visitId, RecordVitalSignRequest request) {
        Visit visit = getVisit(visitId);
        careScope.assertCovers(visit.getPatient().getId());
        if (visit.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot record vitals on a " + visit.getStatus() + " visit");
        }
        assertValidReading(request);

        VitalSign vitalSign = visitMapper.toVitalSign(request);
        vitalSign.setVisit(visit);
        vitalSign.setRecordedAt(Instant.now());
        return visitMapper.toVitalSignResponse(vitalSignRepository.save(vitalSign));
    }

    @Override
    public VitalSignResponse correctVitals(UUID visitId, UUID vitalSignId, RecordVitalSignRequest request) {
        Visit visit = getVisit(visitId);
        careScope.assertCovers(visit.getPatient().getId());
        assertNotCancelled(visit);
        // whoever takes readings can fix them during the visit; after it the record is its doctor's
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            assertAssignedDoctor(visit);
        }
        assertValidReading(request);

        VitalSign vitalSign = vitalSignRepository.findByIdAndVisitId(vitalSignId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Vital sign not found"));
        visitMapper.updateVitalSign(request, vitalSign);
        return visitMapper.toVitalSignResponse(vitalSign);
    }

    @Override
    public Visit getEditableEntity(UUID id) {
        Visit visit = getVisit(id);
        assertAssignedDoctor(visit);
        assertNotCancelled(visit);
        return visit;
    }

    private void fillFromAppointment(Visit visit, CreateVisitRequest request) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (visitRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessRuleException("A visit already exists for this appointment");
        }
        if (!STARTABLE_APPOINTMENT_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot start a visit from an appointment that is " + appointment.getStatus());
        }
        if (opensOnly() && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new BusinessRuleException("The patient must be checked in before their visit is opened");
        }
        if (request.patientId() != null && !request.patientId().equals(appointment.getPatient().getId())) {
            throw new BusinessRuleException("Patient does not match the appointment");
        }
        if (StringUtils.hasText(request.doctorId())
                && !request.doctorId().equals(appointment.getDoctor().getUserId())) {
            throw new BusinessRuleException("Doctor does not match the appointment");
        }

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        visit.setAppointment(appointment);
        visit.setPatient(appointment.getPatient());
        visit.setDoctor(appointment.getDoctor());
        visit.setChiefComplaint(appointment.getReason());
    }

    private void fillWalkIn(Visit visit, CreateVisitRequest request) {
        if (request.patientId() == null) {
            throw new BusinessRuleException("patientId is required when the visit has no appointment");
        }
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        if (!patient.isActive()) {
            throw new BusinessRuleException("Patient account is inactive");
        }

        if (opensOnly() && !StringUtils.hasText(request.doctorId())) {
            throw new BusinessRuleException("doctorId is required to register a walk-in");
        }
        String doctorId = StringUtils.hasText(request.doctorId()) ? request.doctorId() : AuthUtils.extractUserId();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        visit.setPatient(patient);
        visit.setDoctor(doctor);
    }

    /**
     * The caller may open visits but not treat patients (visit:open without visit:write): a nurse
     * opening a checked-in patient's visit to take vitals, or the front desk registering a walk-in
     * for a doctor. The doctor's own Start does the same with fewer limits.
     */
    private static boolean opensOnly() {
        return AuthUtils.hasAuthority(Permissions.VISIT_OPEN) && !AuthUtils.hasAuthority(Permissions.VISIT_WRITE);
    }

    private Visit getVisit(UUID id) {
        return visitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
    }

    private static void assertNotCancelled(Visit visit) {
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            throw new BusinessRuleException("Visit is CANCELLED and can no longer be changed");
        }
    }

    private static void assertValidReading(RecordVitalSignRequest request) {
        if (request.isEmpty()) {
            throw new BusinessRuleException("Record at least one measurement");
        }
        if (request.systolicBp() != null && request.diastolicBp() != null
                && request.systolicBp() <= request.diastolicBp()) {
            throw new BusinessRuleException("Systolic pressure must be higher than diastolic pressure");
        }
    }

    private void assertAssignedDoctor(Visit visit) {
        if (AuthUtils.hasAuthority(Roles.ADMIN)) {
            return;
        }
        if (!visit.getDoctor().getUserId().equals(AuthUtils.extractUserId())) {
            throw new AccessDeniedException("Only the visit's doctor can change this visit");
        }
    }

    private void assertCanMove(Visit visit, VisitStatus target) {
        if (!visit.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Cannot move a visit from " + visit.getStatus() + " to " + target);
        }
    }

    private void moveTo(Visit visit, VisitStatus target) {
        assertCanMove(visit, target);
        visit.setStatus(target);
    }
}
