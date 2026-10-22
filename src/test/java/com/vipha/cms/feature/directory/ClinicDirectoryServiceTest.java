package com.vipha.cms.feature.directory;

import com.vipha.cms.feature.department.DepartmentService;
import com.vipha.cms.feature.department.dto.DepartmentResponse;
import com.vipha.cms.feature.doctor.DoctorService;
import com.vipha.cms.feature.doctor.Specialty;
import com.vipha.cms.feature.doctor.dto.DoctorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicDirectoryServiceTest {

    @Mock DepartmentService departmentService;
    @Mock DoctorService doctorService;
    @InjectMocks ClinicDirectoryService service;

    @Test
    void listsEveryDepartmentWithItsBookableDoctors_andOnlyThoseDoctors() {
        when(departmentService.list()).thenReturn(List.of(
                new DepartmentResponse(6L, "General Medicine", "Primary care"),
                new DepartmentResponse(7L, "Cardiology", "Heart care")));
        // the API is asked for active doctors only
        when(doctorService.findAll(isNull(), isNull(), eq(true), eq(0), anyInt())).thenReturn(new PageImpl<>(List.of(
                new DoctorResponse("d2", "Kanya", "Boonmee", true, 6L, "General Medicine", Set.of(Specialty.GENERAL_PRACTITIONER)),
                new DoctorResponse("d1", "Somchai", "Raksa", true, 6L, "General Medicine", Set.of(Specialty.GENERAL_PRACTITIONER)))));

        var directory = service.directory();

        assertThat(directory.departments()).extracting(d -> d.name() + ": " + d.doctors())
                .containsExactly("General Medicine: 2", "Cardiology: 0");
        assertThat(directory.doctors()).extracting(d -> d.lastName()).containsExactly("Boonmee", "Raksa");
    }
}
