package com.vipha.cms.feature.doctor;

import com.vipha.cms.feature.doctor.dto.DoctorResponse;
import com.vipha.cms.feature.doctor.dto.ScheduleEntryDto;
import com.vipha.cms.feature.doctor.dto.SetDepartmentRequest;
import com.vipha.cms.feature.doctor.dto.SetScheduleRequest;
import com.vipha.cms.feature.doctor.dto.SetSpecialtiesRequest;
import com.vipha.cms.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    /** e.g. {@code ?departmentId=6&active=true} for the doctors a patient can book. */
    @GetMapping
    public Page<DoctorResponse> getAll(@RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) Specialty specialty,
                                       @RequestParam(required = false) Boolean active,
                                       @RequestParam(required = false, defaultValue = "0") int pageNumber,
                                       @RequestParam(required = false,defaultValue = "10") int pageSize) {

        return doctorService.findAll(departmentId, specialty, active, pageNumber, pageSize);
    }

    /** The calling doctor's own profile. */
    @GetMapping("/me")
    public DoctorResponse getMine() {
        return doctorService.get(AuthUtils.extractUserId());
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable("id") String userId) {
        return doctorService.get(userId);
    }

    @PostMapping("/{id}/activate")
    public DoctorResponse activate(@PathVariable("id") String userId) {
        return doctorService.activate(userId);
    }

    @PostMapping("/{id}/deactivate")
    public DoctorResponse deactivate(@PathVariable("id") String userId) {
        return doctorService.deactivate(userId);
    }

    @PutMapping("/{id}/department")
    public DoctorResponse setDepartment(
            @PathVariable("id") String userId,
            @Valid @RequestBody SetDepartmentRequest request
    ) {
        return doctorService.setDepartment(userId, request.departmentId());
    }

    @PutMapping("/{id}/specialties")
    public DoctorResponse setSpecialties(
            @PathVariable("id") String userId,
            @Valid @RequestBody SetSpecialtiesRequest request
    ) {
        return doctorService.setSpecialties(userId, request.specialties());
    }

    @GetMapping("/{id}/schedule")
    public List<ScheduleEntryDto> getSchedule(@PathVariable("id") String userId) {
        return doctorService.getSchedule(userId);
    }

    @PutMapping("/{id}/schedule")
    public List<ScheduleEntryDto> setSchedule(
            @PathVariable("id") String userId,
            @Valid @RequestBody SetScheduleRequest request
    ) {
        return doctorService.setSchedule(userId, request);
    }
}
