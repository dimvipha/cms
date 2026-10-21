package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.feature.pharmacy.dto.AddMedicineBatchRequest;
import com.vipha.cms.feature.pharmacy.dto.CreateMedicineRequest;
import com.vipha.cms.feature.pharmacy.dto.MedicineBatchResponse;
import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
import com.vipha.cms.feature.pharmacy.dto.UpdateMedicineRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public Page<MedicineResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return medicineService.search(search, pageNumber, pageSize);
    }

    /** Medicines to reorder: unexpired stock below their reorder level. */
    @GetMapping("/low-stock")
    public List<MedicineResponse> lowStock() {
        return medicineService.findLowStock();
    }

    @GetMapping("/{id}")
    public MedicineResponse get(@PathVariable UUID id) {
        return medicineService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineResponse create(@Valid @RequestBody CreateMedicineRequest request) {
        return medicineService.create(request);
    }

    @PutMapping("/{id}")
    public MedicineResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMedicineRequest request) {
        return medicineService.update(id, request);
    }

    @GetMapping("/{id}/batches")
    public List<MedicineBatchResponse> batches(@PathVariable UUID id) {
        return medicineService.listBatches(id);
    }

    @PostMapping("/{id}/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineBatchResponse addBatch(@PathVariable UUID id, @Valid @RequestBody AddMedicineBatchRequest request) {
        return medicineService.addBatch(id, request);
    }
}
