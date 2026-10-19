package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.feature.pharmacy.dto.MedicineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/{id}")
    public MedicineResponse get(@PathVariable UUID id) {
        return medicineService.get(id);
    }
}
