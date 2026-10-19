package com.vipha.cms.feature.billing.util;

import com.vipha.cms.feature.billing.Invoice;
import com.vipha.cms.feature.billing.InvoiceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class InvoiceSpecs {

    private InvoiceSpecs() {}

    public static Specification<Invoice> patient(UUID patientId) {
        return patientId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId);
    }

    public static Specification<Invoice> visit(UUID visitId) {
        return visitId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("visit").get("id"), visitId);
    }

    public static Specification<Invoice> status(InvoiceStatus status) {
        return status == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
