package com.vipha.cms.feature.doctor;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.department.Department;
import com.vipha.cms.feature.department.DepartmentService;
import com.vipha.cms.feature.doctor.dto.DoctorResponse;
import com.vipha.cms.feature.doctor.dto.ScheduleEntryDto;
import com.vipha.cms.feature.doctor.dto.SetScheduleRequest;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.Permissions;
import com.vipha.cms.security.PersonName;
import com.vipha.cms.security.UserNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentService departmentService;
    private final UserNames userNames;


    @Override
    public void createDoctorProfile(String userId) {
        if (doctorRepository.existsById(userId)) {
            return;
        }
        Doctor doctor = new Doctor();
        doctor.setUserId(userId);
        doctor.setActive(false);
        doctorRepository.save(doctor);
        log.info("Doctor profile created for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse get(String userId) {
        return toResponse(getDoctor(userId));
    }

    @Override
    @Transactional
    public DoctorResponse activate(String userId) {
        Doctor doctor = getDoctor(userId);
        assertReadyForActivation(doctor);
        doctor.setActive(true);
        return toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse deactivate(String userId) {
        Doctor doctor = getDoctor(userId);
        doctor.setActive(false);
        return toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse setDepartment(String userId, Long departmentId) {
        Doctor doctor = getDoctor(userId);
        Department department = departmentId == null ? null : departmentService.getEntity(departmentId);
        doctor.setDepartment(department);
        return toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse setSpecialties(String userId, Set<Specialty> specialties) {
        Doctor doctor = getDoctor(userId);
        doctor.setSpecialties(new HashSet<>(specialties));
        return toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleEntryDto> getSchedule(String userId) {
        return getDoctor(userId).getSchedules().stream()
                .sorted(scheduleOrder())
                .map(s -> new ScheduleEntryDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
    }

    @Override
    @Transactional
    public List<ScheduleEntryDto> setSchedule(String userId, SetScheduleRequest request) {
        if (!AuthUtils.hasAuthority(Permissions.DOCTOR_WRITE) && !userId.equals(AuthUtils.extractUserId())) {
            throw new AccessDeniedException("You can only change your own schedule");
        }
        validateSchedule(request.entries());

        Doctor doctor = getDoctor(userId);
        doctor.getSchedules().clear();
        for (ScheduleEntryDto entry : request.entries()) {
            DoctorSchedule s = new DoctorSchedule();
            s.setDoctor(doctor);
            s.setDayOfWeek(entry.dayOfWeek());
            s.setStartTime(entry.startTime());
            s.setEndTime(entry.endTime());
            doctor.getSchedules().add(s);
        }
        return getSchedule(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<DoctorResponse> findAll(Long departmentId, Specialty specialty, Boolean active,
                                        int pageNumber, int pageSize) {
        Specification<Doctor> spec = Specification.allOf(
                departmentId == null ? Specification.unrestricted()
                        : (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId),
                specialty == null ? Specification.unrestricted()
                        : (root, query, cb) -> cb.isMember(specialty, root.<Set<Specialty>>get("specialties")),
                active == null ? Specification.unrestricted()
                        : (root, query, cb) -> cb.equal(root.get("active"), active));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("userId"));
        return doctorRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Doctor getDoctor(String userId) {
        return doctorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
    }

    private DoctorResponse toResponse(Doctor doctor) {
        PersonName name = userNames.find(doctor.getUserId()).orElse(new PersonName(null, null));
        Department department = doctor.getDepartment();
        return new DoctorResponse(
                doctor.getUserId(),
                name.firstName(),
                name.lastName(),
                doctor.isActive(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName(),
                new HashSet<>(doctor.getSpecialties())
        );
    }

    private void assertReadyForActivation(Doctor doctor) {
        List<String> missing = new ArrayList<>();
        if (doctor.getDepartment() == null) missing.add("department");
        if (doctor.getSpecialties().isEmpty()) missing.add("specialty");
        if (doctor.getSchedules().isEmpty()) missing.add("schedule");
        if (!missing.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot activate doctor. Missing: " + String.join(", ", missing));
        }
    }

    private Comparator<DoctorSchedule> scheduleOrder() {
        return Comparator
                .comparing(DoctorSchedule::getDayOfWeek)
                .thenComparing(DoctorSchedule::getStartTime);
    }

    private void validateSchedule(List<ScheduleEntryDto> entries) {
        for (ScheduleEntryDto entry : entries) {
            if (!entry.startTime().isBefore(entry.endTime())) {
                throw new BusinessRuleException(
                        "Schedule entry startTime must be before endTime for " + entry.dayOfWeek());
            }
        }

        Map<DayOfWeek, List<ScheduleEntryDto>> byDay = new EnumMap<>(DayOfWeek.class);
        for (ScheduleEntryDto entry : entries) {
            byDay.computeIfAbsent(entry.dayOfWeek(), d -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<DayOfWeek, List<ScheduleEntryDto>> perDay : byDay.entrySet()) {
            List<ScheduleEntryDto> sorted = perDay.getValue().stream()
                    .sorted(Comparator.comparing(ScheduleEntryDto::startTime))
                    .toList();
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                    throw new BusinessRuleException(
                            "Overlapping schedule entries for " + perDay.getKey());
                }
            }
        }
    }
}
