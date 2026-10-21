package com.vipha.cms.feature.appointment.util;

import com.vipha.cms.feature.appointment.Appointment;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class AppointmentSpecs {

    private AppointmentSpecs() {}

    public static Specification<Appointment> patient(UUID patientId) {
        return patientId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Appointment> doctor(String doctorId) {
        return doctorId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("doctor").get("userId"), doctorId);
    }

    public static Specification<Appointment> status(AppointmentStatus status) {
        return status == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> onDate(LocalDate date) {
        return date == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("appointmentDate"), date);
    }
}
