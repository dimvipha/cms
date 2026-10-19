package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MedicineService {

    Page<MedicineResponse> search(String query, int pageNumber, int pageSize);

    MedicineResponse get(UUID id);
}
