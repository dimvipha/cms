package com.vipha.cms.feature.doctor;

import com.vipha.cms.feature.doctor.dto.DoctorResponse;
import com.vipha.cms.feature.doctor.dto.ScheduleEntryDto;
import com.vipha.cms.feature.doctor.dto.SetScheduleRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface DoctorService {

    void createDoctorProfile(String userId);

    DoctorResponse get(String userId);

    DoctorResponse activate(String userId);

    DoctorResponse deactivate(String userId);

    DoctorResponse setDepartment(String userId, Long departmentId);

    DoctorResponse setSpecialties(String userId, Set<Specialty> specialties);

    List<ScheduleEntryDto> getSchedule(String userId);

    List<ScheduleEntryDto> setSchedule(String userId, SetScheduleRequest request);

    Page<DoctorResponse> findAll(int pageNumber, int pageSize);
}
