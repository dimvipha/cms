package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.pharmacy.dto.AddMedicineBatchRequest;
import com.vipha.cms.feature.pharmacy.dto.CreateMedicineRequest;
import com.vipha.cms.feature.pharmacy.dto.MedicineBatchResponse;
import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
import com.vipha.cms.feature.pharmacy.dto.UpdateMedicineRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final ClinicProperties clinicProperties;

    @Override
    public Page<MedicineResponse> search(String query, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("name").ascending());
        Page<Medicine> page = StringUtils.hasText(query)
                ? medicineRepository.search(query.trim(), pageable)
                : medicineRepository.findByActiveTrue(pageable);

        Map<UUID, Integer> stock = usableStock(page.map(Medicine::getId).getContent());
        return page.map(m -> toResponse(m, stock.getOrDefault(m.getId(), 0)));
    }

    @Override
    public MedicineResponse get(UUID id) {
        Medicine medicine = getMedicine(id);
        return toResponse(medicine, usableStock(List.of(id)).getOrDefault(id, 0));
    }

    @Override
    @Transactional
    public MedicineResponse create(CreateMedicineRequest request) {
        if (medicineRepository.existsByCode(request.code())) {
            throw new BusinessRuleException("A medicine with code " + request.code() + " already exists");
        }
        Medicine medicine = new Medicine();
        medicine.setCode(request.code());
        medicine.setName(request.name());
        medicine.setUnit(request.unit());
        medicine.setForm(request.form());
        medicine.setPrice(request.price());
        medicine.setReorderLevel(request.reorderLevel());
        medicine.setActive(true);
        return toResponse(medicineRepository.save(medicine), 0);
    }

    @Override
    @Transactional
    public MedicineResponse update(UUID id, UpdateMedicineRequest request) {
        Medicine medicine = getMedicine(id);
        if (StringUtils.hasText(request.name())) medicine.setName(request.name());
        if (StringUtils.hasText(request.unit())) medicine.setUnit(request.unit());
        if (request.form() != null) medicine.setForm(request.form());
        if (request.price() != null) medicine.setPrice(request.price());
        if (request.reorderLevel() != null) medicine.setReorderLevel(request.reorderLevel());
        if (request.active() != null) medicine.setActive(request.active());
        return toResponse(medicine, usableStock(List.of(id)).getOrDefault(id, 0));
    }

    @Override
    public List<MedicineBatchResponse> listBatches(UUID medicineId) {
        getMedicine(medicineId);
        return medicineBatchRepository.findByMedicineIdOrderByExpiryDateAsc(medicineId).stream()
                .map(this::toBatchResponse)
                .toList();
    }

    @Override
    @Transactional
    public MedicineBatchResponse addBatch(UUID medicineId, AddMedicineBatchRequest request) {
        Medicine medicine = getMedicine(medicineId);
        if (medicineBatchRepository.existsByMedicineIdAndBatchNumber(medicineId, request.batchNumber())) {
            throw new BusinessRuleException("Batch " + request.batchNumber() + " is already recorded for this medicine");
        }
        MedicineBatch batch = new MedicineBatch();
        batch.setMedicine(medicine);
        batch.setBatchNumber(request.batchNumber());
        batch.setExpiryDate(request.expiryDate());
        batch.setStockQuantity(request.stockQuantity());
        batch.setReceivedAt(request.receivedAt());
        return toBatchResponse(medicineBatchRepository.save(batch));
    }

    @Override
    public List<MedicineResponse> findLowStock() {
        List<Medicine> medicines = medicineRepository.findByActiveTrue(Sort.by("name"));
        Map<UUID, Integer> stock = usableStock(medicines.stream().map(Medicine::getId).toList());
        return medicines.stream()
                .filter(m -> stock.getOrDefault(m.getId(), 0) < m.getReorderLevel())
                .map(m -> toResponse(m, stock.getOrDefault(m.getId(), 0)))
                .toList();
    }

    private Medicine getMedicine(UUID id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
    }

    private MedicineBatchResponse toBatchResponse(MedicineBatch batch) {
        return new MedicineBatchResponse(
                batch.getId(),
                batch.getMedicine().getId(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                batch.getStockQuantity(),
                batch.getReceivedAt(),
                batch.getCreatedAt()
        );
    }

    private Map<UUID, Integer> usableStock(List<UUID> medicineIds) {
        if (medicineIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(clinicProperties.timeZone());
        return medicineBatchRepository.sumUsableStock(medicineIds, today).stream()
                .collect(Collectors.toMap(
                        MedicineBatchRepository.MedicineStock::getMedicineId,
                        s -> Math.toIntExact(s.getQuantity())));
    }

    private MedicineResponse toResponse(Medicine m, int totalStock) {
        return new MedicineResponse(
                m.getId(),
                m.getCode(),
                m.getName(),
                m.getUnit(),
                m.getForm(),
                m.getPrice(),
                m.getReorderLevel(),
                totalStock,
                m.isActive(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
