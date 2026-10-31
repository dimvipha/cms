package com.vipha.cms.feature.care;

import com.vipha.cms.feature.appointment.Appointment;
import com.vipha.cms.feature.appointment.AppointmentRepository;
import com.vipha.cms.feature.appointment.AppointmentService;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.feature.patient.PatientService;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitRepository;
import com.vipha.cms.feature.visit.VisitService;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import com.vipha.cms.security.Roles;
import com.vipha.cms.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The care rule's list queries against the database: a doctor's patient search and visit list
 * hold only their own patients. Every test rolls back, so the database is left as it was.
 */
@SpringBootTest
@Transactional
class CareScopeQueryTest {

    private static final String SURNAME = "Caretest" + UUID.randomUUID().toString().substring(0, 8);

    @Autowired PatientRepository patientRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired VisitRepository visitRepository;
    @Autowired PatientService patientService;
    @Autowired VisitService visitService;
    @Autowired AppointmentService appointmentService;

    Doctor me;
    Doctor colleague;
    Patient booked;
    Patient seen;
    Patient cancelled;
    Patient colleagues;

    @BeforeEach
    void setUp() {
        me = doctor("care-test-me-" + UUID.randomUUID());
        colleague = doctor("care-test-colleague-" + UUID.randomUUID());
        booked = patient("Booked");
        seen = patient("Seen");
        cancelled = patient("Cancelled");
        colleagues = patient("Colleagues");

        appointment(booked, me, AppointmentStatus.SCHEDULED);
        appointment(cancelled, me, AppointmentStatus.CANCELLED);
        appointment(colleagues, colleague, AppointmentStatus.COMPLETED);
        visit(seen, me);
        // history: my patient was seen by a colleague too, and the colleague's own patient
        visit(booked, colleague);
        visit(colleagues, colleague);
    }

    @AfterEach
    void tearDown() {
        TestAuth.logout();
    }

    @Test
    void aDoctorFindsOnlyPatientsBookedWithThemOrSeenByThem() {
        TestAuth.loginAs(me.getUserId(), Roles.DOCTOR, "patient:read-all");

        assertThat(searchNames()).containsExactlyInAnyOrder("Booked", "Seen");
    }

    @Test
    void aDoctorWhoIsAlsoANurseFindsOnlyTheirOwnPatients() {
        TestAuth.loginAs(me.getUserId(), Roles.DOCTOR, Roles.NURSE, "patient:read-all");

        assertThat(searchNames()).containsExactlyInAnyOrder("Booked", "Seen");
    }

    @Test
    void aDoctorWhoIsAlsoANurseListsOnlyTheirOwnAppointments() {
        TestAuth.loginAs(me.getUserId(), Roles.DOCTOR, Roles.NURSE, "appointment:read", "appointment:read-all");

        assertThat(appointmentService.findAll(null, null, null, null, 0, 50).getContent())
                .allSatisfy(appointment -> assertThat(appointment.doctorId()).isEqualTo(me.getUserId()))
                .hasSize(2);
    }

    @Test
    void aNurseListsEveryAppointment() {
        TestAuth.loginAs("care-test-nurse", Roles.NURSE, "appointment:read-all");

        assertThat(appointmentService.findAll(null, colleague.getUserId(), null, null, 0, 50).getContent())
                .hasSize(1);
    }

    @Test
    void aNurseFindsEveryPatient() {
        TestAuth.loginAs("care-test-nurse", Roles.NURSE, "patient:read-all");

        assertThat(searchNames()).containsExactlyInAnyOrder("Booked", "Seen", "Cancelled", "Colleagues");
    }

    @Test
    void aDoctorSeesEveryVisitOfTheirPatients_andNoneOfAnyoneElse() {
        TestAuth.loginAs(me.getUserId(), Roles.DOCTOR, "visit:read");

        var visits = visitService.findAll(null, null, null, null, 0, 100).getContent();

        assertThat(visits).extracting(visit -> visit.patient().firstName() + " with " + doctorOf(visit))
                .containsExactlyInAnyOrder("Seen with me", "Booked with colleague");
    }

    private java.util.List<String> searchNames() {
        return patientService.findAll(SURNAME, PageRequest.of(0, 50)).getContent().stream()
                .map(PatientResponse::firstName)
                .toList();
    }

    private String doctorOf(VisitSummaryResponse visit) {
        return visit.doctorId().equals(me.getUserId()) ? "me" : "colleague";
    }

    private Doctor doctor(String userId) {
        Doctor doctor = new Doctor();
        doctor.setUserId(userId);
        doctor.setActive(true);
        return doctorRepository.save(doctor);
    }

    private Patient patient(String firstName) {
        Patient patient = new Patient();
        patient.setMrn("CT-" + UUID.randomUUID().toString().substring(0, 12));
        patient.setFirstName(firstName);
        patient.setLastName(SURNAME);
        patient.setDob(LocalDate.of(1990, 1, 1));
        patient.setGender("OTHER");
        patient.setPhone("+8550000" + (int) (Math.random() * 100000));
        return patientRepository.save(patient);
    }

    private void appointment(Patient patient, Doctor doctor, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setAppointmentTime(LocalTime.of(9, 0));
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    private void visit(Patient patient, Doctor doctor) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setVisitDate(Instant.now());
        visit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(visit);
    }
}
