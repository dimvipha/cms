package com.vipha.cms.feature.pharmacy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    @Query("select m from Medicine m " +
            "where lower(m.name) like lower(concat('%', :q, '%')) " +
            "or lower(m.code) like lower(concat('%', :q, '%'))")
    Page<Medicine> search(@Param("q") String q, Pageable pageable);
}
