package com.vipha.cms.feature.patient.util;

import com.vipha.cms.feature.patient.Patient;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PatientSpecs {

    public static Specification<Patient> search(String q) {
        if (!StringUtils.hasText(q)) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like),
                cb.like(cb.lower(root.get("mrn")), like),
                cb.like(root.get("phone"), like));
    }

    /** Leaves out closed records. */
    public static Specification<Patient> active() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Patient> bloodGroup(String bg) {
        return !StringUtils.hasText(bg) ? null
                : (root, query, cb) -> cb.equal(root.get("bloodGroup"), bg);
    }
}