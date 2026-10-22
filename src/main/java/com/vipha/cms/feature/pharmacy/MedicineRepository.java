package com.vipha.cms.feature.pharmacy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    /** The catalogue: active medicines whose name or code contains {@code q}. */
    @Query("select m from Medicine m " +
            "where m.active = true " +
            "and (lower(m.name) like lower(concat('%', :q, '%')) " +
            "or lower(m.code) like lower(concat('%', :q, '%')))")
    Page<Medicine> search(@Param("q") String q, Pageable pageable);

    Page<Medicine> findByActiveTrue(Pageable pageable);

    List<Medicine> findByActiveTrue(Sort sort);

    /** Includes discontinued medicines: codes stay unique forever. */
    boolean existsByCode(String code);
}
