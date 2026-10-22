package com.vipha.cms.feature.visit;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.appointment.Appointment;
import com.vipha.cms.feature.appointment.AppointmentRepository;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import com.vipha.cms.feature.care.CareScope;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientMapperImpl;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.feature.visit.dto.CreateVisitRequest;
import com.vipha.cms.feature.visit.dto.RecordVitalSignRequest;
import com.vipha.cms.feature.visit.dto.UpdateVisitRequest;
import com.vipha.cms.feature.visit.dto.VisitResponse;
import com.vipha.cms.feature.visit.dto.VitalSignResponse;
import com.vipha.cms.security.Roles;
import com.vipha.cms.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import com.vipha.cms.security.UserNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplTest {

    private static final String DOCTOR_ID = "doctor-1";
    private static final String OTHER_DOCTOR_ID = "doctor-2";

    @Mock UserNames userNames;

    @Mock VisitRepository visitRepository;
    @Mock VitalSignRepository vitalSignRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock CareScope careScope;

    VisitServiceImpl service;

    @BeforeEach
    void setUp() {
        VisitMapperImpl mapper = new VisitMapperImpl();
        ReflectionTestUtils.setField(mapper, "patientMapper", new PatientMapperImpl());
        ReflectionTestUtils.setField(mapper, "userNames", userNames);
        service = new VisitServiceImpl(visitRepository, vitalSignRepository, appointmentRepository,
                patientRepository, doctorRepository, mapper,
                new ClinicProperties("Test Clinic", "", "", ZoneId.of("Asia/Bangkok")), careScope);
        TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR);
    }

    @AfterEach
    void tearDown() {
        TestAuth.logout();
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void createFromAppointment_takesPatientDoctorAndReason_andMarksAppointmentInProgress() {
        Appointment appointment = appointment(AppointmentStatus.CHECKED_IN);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(visitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse response = service.create(new CreateVisitRequest(appointment.getId(), null, null, null));

        assertThat(response.patient().id()).isEqualTo(appointment.getPatient().getId());
        assertThat(response.doctorId()).isEqualTo(DOCTOR_ID);
        assertThat(response.appointmentId()).isEqualTo(appointment.getId());
        assertThat(response.chiefComplaint()).isEqualTo("Fever and cough");
        assertThat(response.status()).isEqualTo(VisitStatus.OPEN);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void createFromAppointment_rejectsSecondVisit() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(visitRepository.existsByAppointmentId(appointment.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateVisitRequest(appointment.getId(), null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createFromAppointment_rejectsCancelledAppointment() {
        Appointment appointment = appointment(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.create(new CreateVisitRequest(appointment.getId(), null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void createFromAppointment_rejectsMismatchedPatient() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.create(
                new CreateVisitRequest(appointment.getId(), UUID.randomUUID(), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Patient does not match");
    }

    @Test
    void createWalkIn_requiresPatient() {
        assertThatThrownBy(() -> service.create(new CreateVisitRequest(null, null, null, "Headache")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("patientId is required");
    }

    @Test
    void createWalkIn_defaultsDoctorToCaller() {
        Patient patient = patient();
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor(DOCTOR_ID)));
        when(visitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse response = service.create(new CreateVisitRequest(null, patient.getId(), null, "Headache"));

        assertThat(response.doctorId()).isEqualTo(DOCTOR_ID);
        assertThat(response.appointmentId()).isNull();
        assertThat(response.chiefComplaint()).isEqualTo("Headache");
    }

    @Test
    void create_rejectsInactiveDoctor() {
        Patient patient = patient();
        Doctor inactive = doctor(DOCTOR_ID);
        inactive.setActive(false);
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.create(new CreateVisitRequest(null, patient.getId(), null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");
    }

    // ── update / ownership ──────────────────────────────────────────────────

    @Test
    void update_recordsDiagnosis_andNormalisesIcdCode() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        VisitResponse response = service.update(visit.getId(),
                new UpdateVisitRequest(null, "j06.9", "Acute upper respiratory infection", null));

        assertThat(response.diagnosisCode()).isEqualTo("J06.9");
        assertThat(response.diagnosis()).isEqualTo("Acute upper respiratory infection");
        assertThat(response.chiefComplaint()).isEqualTo("Cough");
    }

    @Test
    void update_blankIcdCodeClearsIt() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        visit.setDiagnosisCode("J06.9");
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        service.update(visit.getId(), new UpdateVisitRequest(null, "", null, null));

        assertThat(visit.getDiagnosisCode()).isNull();
    }

    @Test
    void update_byAnotherDoctor_isForbidden() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        TestAuth.loginAs(OTHER_DOCTOR_ID, Roles.DOCTOR);

        assertThatThrownBy(() -> service.update(visit.getId(), new UpdateVisitRequest(null, null, "x", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_byAdmin_isAllowed() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        TestAuth.loginAs("admin-1", Roles.ADMIN);

        service.update(visit.getId(), new UpdateVisitRequest(null, null, "Migraine", null));

        assertThat(visit.getDiagnosis()).isEqualTo("Migraine");
    }

    @Test
    void update_onCompletedVisit_isRejected() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.update(visit.getId(), new UpdateVisitRequest(null, null, "x", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("can no longer be changed");
    }

    // ── status transitions ──────────────────────────────────────────────────

    @Test
    void start_movesOpenVisitInProgress() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThat(service.start(visit.getId()).status()).isEqualTo(VisitStatus.IN_PROGRESS);
    }

    @Test
    void complete_requiresVisitToBeStarted() {
        Visit visit = visit(VisitStatus.OPEN);
        visit.setDiagnosis("Common cold");
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.complete(visit.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("OPEN to COMPLETED");
    }

    @Test
    void complete_requiresDiagnosis() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.complete(visit.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("diagnosis");
        assertThat(visit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
    }

    @Test
    void complete_closesVisitAndAppointment() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        visit.setDiagnosis("Common cold");
        Appointment appointment = appointment(AppointmentStatus.IN_PROGRESS);
        visit.setAppointment(appointment);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThat(service.complete(visit.getId()).status()).isEqualTo(VisitStatus.COMPLETED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void cancel_closesVisitAndAppointment() {
        Visit visit = visit(VisitStatus.OPEN);
        Appointment appointment = appointment(AppointmentStatus.IN_PROGRESS);
        visit.setAppointment(appointment);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThat(service.cancel(visit.getId()).status()).isEqualTo(VisitStatus.CANCELLED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_completedVisit_isRejected() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.cancel(visit.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── vitals ──────────────────────────────────────────────────────────────

    @Test
    void recordVitals_savesReadingWithBmi() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(vitalSignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VitalSignResponse response = service.recordVitals(visit.getId(), new RecordVitalSignRequest(
                120, 80, 72, 16, new BigDecimal("36.8"), new BigDecimal("70.0"), new BigDecimal("175.0"), 98));

        assertThat(response.visitId()).isEqualTo(visit.getId());
        assertThat(response.recordedAt()).isNotNull();
        assertThat(response.bmi()).isEqualByComparingTo("22.9");
    }

    @Test
    void recordVitals_rejectsClosedVisit() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.recordVitals(visit.getId(), reading(120, 80)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void recordVitals_rejectsEmptyReading() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.recordVitals(visit.getId(),
                new RecordVitalSignRequest(null, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void recordVitals_rejectsSystolicNotAboveDiastolic() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.recordVitals(visit.getId(), reading(80, 80)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Systolic");
        verify(vitalSignRepository, never()).save(any());
    }

    @Test
    void recordVitals_isNotLimitedToTheVisitsDoctor() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(vitalSignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TestAuth.loginAs("nurse-1", Roles.NURSE);

        assertThat(service.recordVitals(visit.getId(), reading(118, 76))).isNotNull();
    }

    // ── a doctor's own patients ─────────────────────────────────────────────

    @Test
    void get_refusesAPatientOutsideTheDoctorsCare() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        doThrow(new AccessDeniedException("You can only see patients in your care"))
                .when(careScope).assertCovers(visit.getPatient().getId());

        assertThatThrownBy(() -> service.get(visit.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void get_letsADoctorReadAnotherDoctorsVisitOfTheirPatient() {
        Visit visit = visit(VisitStatus.COMPLETED);
        visit.setDoctor(doctor(OTHER_DOCTOR_ID));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThat(service.get(visit.getId())).isNotNull();
        verify(careScope).assertCovers(visit.getPatient().getId());
    }

    @Test
    void recordVitals_refusesAPatientOutsideTheDoctorsCare() {
        Visit visit = visit(VisitStatus.OPEN);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        doThrow(new AccessDeniedException("You can only see patients in your care"))
                .when(careScope).assertCovers(visit.getPatient().getId());

        assertThatThrownBy(() -> service.recordVitals(visit.getId(), reading(118, 76)))
                .isInstanceOf(AccessDeniedException.class);
        verify(vitalSignRepository, never()).save(any());
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    private static RecordVitalSignRequest reading(int systolic, int diastolic) {
        return new RecordVitalSignRequest(systolic, diastolic, null, null, null, null, null, null);
    }

    private static Doctor doctor(String id) {
        Doctor doctor = new Doctor();
        doctor.setUserId(id);
        doctor.setActive(true);
        return doctor;
    }

    private static Patient patient() {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setMrn("P-2026-00001");
        patient.setFirstName("Somsak");
        patient.setLastName("Jaidee");
        patient.setDob(LocalDate.of(1985, 3, 12));
        patient.setGender("MALE");
        return patient;
    }

    private static Appointment appointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient());
        appointment.setDoctor(doctor(DOCTOR_ID));
        appointment.setReason("Fever and cough");
        appointment.setStatus(status);
        return appointment;
    }

    private static Visit visit(VisitStatus status) {
        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient());
        visit.setDoctor(doctor(DOCTOR_ID));
        visit.setVisitDate(Instant.now());
        visit.setChiefComplaint("Cough");
        visit.setStatus(status);
        return visit;
    }
}
