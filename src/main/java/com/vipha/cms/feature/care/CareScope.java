package com.vipha.cms.feature.care;

import com.vipha.cms.feature.appointment.Appointment;
import com.vipha.cms.feature.appointment.AppointmentRepository;
import com.vipha.cms.feature.appointment.AppointmentStatus;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitRepository;
import com.vipha.cms.security.AuthUtils;
import com.vipha.cms.security.Roles;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * A doctor works with their own patients: anyone booked with them (an appointment that is not
 * cancelled) or seen by them in a visit. For those patients they read the whole record, other
 * doctors' visits and prescriptions included, because treatment needs the history; the rest of
 * the clinic stays closed to them. Staff whose work spans the clinic are not limited, but an
 * account that is also a doctor works as one; only an admin stays clinic-wide.
 */
@Component
@RequiredArgsConstructor
public class CareScope {

    private static final String NOT_IN_CARE = "You can only see patients in your care";

    private final AppointmentRepository appointmentRepository;
    private final VisitRepository visitRepository;

    /** The doctor whose patients the caller is limited to; empty when the caller sees the whole clinic. */
    public Optional<String> limitedToDoctor() {
        boolean limited = AuthUtils.hasAuthority(Roles.DOCTOR) && !AuthUtils.hasAuthority(Roles.ADMIN);
        return limited ? Optional.of(AuthUtils.extractUserId()) : Optional.empty();
    }

    /** Refuses a single record of a patient outside a limited doctor's care. */
    public void assertCovers(UUID patientId) {
        limitedToDoctor().ifPresent(doctorId -> {
            if (!caresFor(doctorId, patientId)) {
                throw new AccessDeniedException(NOT_IN_CARE);
            }
        });
    }

    boolean caresFor(String doctorUserId, UUID patientId) {
        return appointmentRepository.existsByDoctorUserIdAndPatientIdAndStatusNot(
                        doctorUserId, patientId, AppointmentStatus.CANCELLED)
                || visitRepository.existsByDoctorUserIdAndPatientId(doctorUserId, patientId);
    }

    /**
     * Narrows a list to a limited doctor's patients, given where each row keeps its patient's id;
     * lets everyone else's lists through unchanged.
     */
    public <T> Specification<T> narrow(Function<Root<T>, Expression<UUID>> patientIdOf) {
        return limitedToDoctor()
                .<Specification<T>>map(doctorId -> (root, query, cb) ->
                        inCareOf(patientIdOf.apply(root), doctorId, query, cb))
                .orElse(Specification.unrestricted());
    }

    private static Predicate inCareOf(Expression<UUID> patientId, String doctorUserId,
                                      CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> booked = query.subquery(UUID.class);
        Root<Appointment> appointment = booked.from(Appointment.class);
        booked.select(appointment.get("patient").get("id")).where(
                cb.equal(appointment.get("doctor").get("userId"), doctorUserId),
                cb.notEqual(appointment.get("status"), AppointmentStatus.CANCELLED));

        Subquery<UUID> seen = query.subquery(UUID.class);
        Root<Visit> visit = seen.from(Visit.class);
        seen.select(visit.get("patient").get("id")).where(
                cb.equal(visit.get("doctor").get("userId"), doctorUserId));

        return cb.or(patientId.in(booked), patientId.in(seen));
    }
}
