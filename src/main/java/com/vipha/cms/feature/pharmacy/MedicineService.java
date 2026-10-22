package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.feature.pharmacy.dto.AddMedicineBatchRequest;
import com.vipha.cms.feature.pharmacy.dto.CreateMedicineRequest;
import com.vipha.cms.feature.pharmacy.dto.MedicineBatchResponse;
import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
import com.vipha.cms.feature.pharmacy.dto.UpdateMedicineRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface MedicineService {

    Page<MedicineResponse> search(String query, int pageNumber, int pageSize);

    MedicineResponse get(UUID id);

    MedicineResponse create(CreateMedicineRequest request);

    /** Partial update; {@code active=false} discontinues the medicine, which then drops out of the catalogue. */
    MedicineResponse update(UUID id, UpdateMedicineRequest request);

    /** All batches of a medicine, soonest expiry first, expired ones included. */
    List<MedicineBatchResponse> listBatches(UUID medicineId);

    /** Received stock: adds a batch with its own expiry date. */
    MedicineBatchResponse addBatch(UUID medicineId, AddMedicineBatchRequest request);

    /** Medicines whose unexpired stock is below their reorder level. */
    List<MedicineResponse> findLowStock();
}
