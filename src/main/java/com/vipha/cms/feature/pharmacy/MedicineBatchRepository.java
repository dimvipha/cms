package com.vipha.cms.feature.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, UUID> {

    interface MedicineStock {
        UUID getMedicineId();
        Long getQuantity();
    }

    /** Usable (unexpired) stock per medicine; medicines with no usable stock are absent. */
    @Query("select b.medicine.id as medicineId, sum(b.stockQuantity) as quantity " +
            "from MedicineBatch b " +
            "where b.medicine.id in :medicineIds and b.expiryDate >= :today " +
            "group by b.medicine.id")
    List<MedicineStock> sumUsableStock(@Param("medicineIds") Collection<UUID> medicineIds,
                                       @Param("today") LocalDate today);

    List<MedicineBatch> findByMedicineIdOrderByExpiryDateAsc(UUID medicineId);

    boolean existsByMedicineIdAndBatchNumber(UUID medicineId, String batchNumber);
}
