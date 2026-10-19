package com.vipha.cms.feature.visit.util;

import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class VisitSpecs {

    private VisitSpecs() {}

    public static Specification<Visit> patient(UUID patientId) {
        return patientId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Visit> doctor(String doctorId) {
        return !StringUtils.hasText(doctorId) ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("doctor").get("userId"), doctorId);
    }

    public static Specification<Visit> status(VisitStatus status) {
        return status == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Visits whose visitDate falls on the given calendar day in the clinic's zone. */
    public static Specification<Visit> onDate(LocalDate date, ZoneId zone) {
        if (date == null) return Specification.unrestricted();
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("visitDate"), from),
                cb.lessThan(root.get("visitDate"), to));
    }
}
