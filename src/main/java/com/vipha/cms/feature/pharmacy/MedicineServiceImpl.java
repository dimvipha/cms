package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
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
                : medicineRepository.findAll(pageable);

        Map<UUID, Integer> stock = usableStock(page.map(Medicine::getId).getContent());
        return page.map(m -> toResponse(m, stock.getOrDefault(m.getId(), 0)));
    }

    @Override
    public MedicineResponse get(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        return toResponse(medicine, usableStock(List.of(id)).getOrDefault(id, 0));
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
