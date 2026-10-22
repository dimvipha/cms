package com.vipha.cms.feature.directory;

import com.vipha.cms.feature.department.DepartmentService;
import com.vipha.cms.feature.directory.dto.ClinicDirectoryResponse;
import com.vipha.cms.feature.doctor.DoctorService;
import com.vipha.cms.feature.doctor.dto.DoctorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicDirectoryService {

    /** More than a clinic has; the list is not paged. */
    private static final int ALL = 500;

    private final DepartmentService departmentService;
    private final DoctorService doctorService;

    /** Every department, and only the doctors patients can book: inactive ones stay unlisted. */
    public ClinicDirectoryResponse directory() {
        List<DoctorResponse> bookable = doctorService.findAll(null, null, true, 0, ALL).getContent();
        Map<Long, Long> perDepartment = bookable.stream()
                .map(DoctorResponse::departmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<ClinicDirectoryResponse.Department> departments = departmentService.list().stream()
                .map(department -> new ClinicDirectoryResponse.Department(
                        department.id(),
                        department.name(),
                        department.description(),
                        perDepartment.getOrDefault(department.id(), 0L).intValue()))
                .toList();

        List<ClinicDirectoryResponse.Doctor> doctors = bookable.stream()
                .sorted(Comparator.comparing(DoctorResponse::lastName, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(doctor -> new ClinicDirectoryResponse.Doctor(
                        doctor.userId(),
                        doctor.firstName(),
                        doctor.lastName(),
                        doctor.departmentId(),
                        doctor.departmentName(),
                        doctor.specialties()))
                .toList();

        return new ClinicDirectoryResponse(departments, doctors);
    }
}
