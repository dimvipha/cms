package com.vipha.cms.feature.directory.dto;

import com.vipha.cms.feature.doctor.Specialty;

import java.util.List;
import java.util.Set;

/** What the clinic shows visitors before they sign in: its departments and the doctors taking bookings. */
public record ClinicDirectoryResponse(
        List<Department> departments,
        List<Doctor> doctors
) {
    /** A department, with how many doctors in it take bookings. */
    public record Department(Long id, String name, String description, int doctors) {}

    public record Doctor(
            String id,
            String firstName,
            String lastName,
            Long departmentId,
            String departmentName,
            Set<Specialty> specialties
    ) {}
}
