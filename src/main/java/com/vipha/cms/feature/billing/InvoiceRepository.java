package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    long nextInvoiceSequence();

    boolean existsByVisitIdAndStatusNot(UUID visitId, InvoiceStatus status);

    /** Row-locks the invoice so concurrent payments cannot both pass the balance check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findForUpdateById(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = "patient")
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    /** Visits in the given status that have no invoice yet, or only voided ones. */
    @Query(value = "select v from Visit v join fetch v.patient " +
            "where v.status = :status and not exists " +
            "(select i.id from Invoice i where i.visit = v and i.status <> :voided)",
            countQuery = "select count(v) from Visit v " +
                    "where v.status = :status and not exists " +
                    "(select i.id from Invoice i where i.visit = v and i.status <> :voided)")
    Page<Visit> findUnbilledVisits(@Param("status") VisitStatus status,
                                   @Param("voided") InvoiceStatus voided,
                                   Pageable pageable);
}
