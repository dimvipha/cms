package com.vipha.cms.feature.prescription.util;

import com.vipha.cms.feature.prescription.Prescription;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class PrescriptionSpecs {

    private PrescriptionSpecs() {}

    public static Specification<Prescription> patient(UUID patientId) {
        return patientId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Prescription> visit(UUID visitId) {
        return visitId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("visit").get("id"), visitId);
    }
}
