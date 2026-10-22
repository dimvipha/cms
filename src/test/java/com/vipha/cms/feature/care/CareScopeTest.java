package com.vipha.cms.feature.care;

import com.vipha.cms.feature.appointment.AppointmentRepository;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import com.vipha.cms.feature.visit.VisitRepository;
import com.vipha.cms.security.Roles;
import com.vipha.cms.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareScopeTest {

    private static final String DOCTOR_ID = "doctor-1";

    @Mock AppointmentRepository appointmentRepository;
    @Mock VisitRepository visitRepository;

    CareScope careScope;
    final UUID patientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        careScope = new CareScope(appointmentRepository, visitRepository);
    }

    @AfterEach
    void tearDown() {
        TestAuth.logout();
    }

    @Test
    void aDoctorIsLimitedToTheirOwnPatients() {
        TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR, "patient:read-all");
        assertThat(careScope.limitedToDoctor()).contains(DOCTOR_ID);
    }

    @Test
    void clinicWideRolesAreNotLimited_evenWhenAlsoADoctor() {
        for (String role : new String[]{Roles.NURSE, Roles.RECEPTIONIST, Roles.PHARMACIST, Roles.LAB_TECHNICIAN, Roles.ADMIN}) {
            TestAuth.loginAs("someone", role);
            assertThat(careScope.limitedToDoctor()).as(role).isEmpty();
            TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR, role);
            assertThat(careScope.limitedToDoctor()).as("DOCTOR and " + role).isEmpty();
        }
    }

    @Test
    void aPatientBookedWithTheDoctorIsInTheirCare() {
        TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR);
        when(appointmentRepository.existsByDoctorUserIdAndPatientIdAndStatusNot(
                DOCTOR_ID, patientId, AppointmentStatus.CANCELLED)).thenReturn(true);

        assertThatCode(() -> careScope.assertCovers(patientId)).doesNotThrowAnyException();
    }

    @Test
    void aPatientTheDoctorHasSeenIsInTheirCare() {
        TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR);
        when(visitRepository.existsByDoctorUserIdAndPatientId(DOCTOR_ID, patientId)).thenReturn(true);

        assertThatCode(() -> careScope.assertCovers(patientId)).doesNotThrowAnyException();
    }

    @Test
    void anyOtherPatientIsRefused() {
        TestAuth.loginAs(DOCTOR_ID, Roles.DOCTOR);

        assertThatThrownBy(() -> careScope.assertCovers(patientId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only see patients in your care");
    }

    @Test
    void clinicWideStaffAreNotChecked() {
        TestAuth.loginAs("nurse-1", Roles.NURSE);

        careScope.assertCovers(patientId);

        verify(appointmentRepository, never()).existsByDoctorUserIdAndPatientIdAndStatusNot(anyString(), any(), any());
        verify(visitRepository, never()).existsByDoctorUserIdAndPatientId(anyString(), any());
    }
}
