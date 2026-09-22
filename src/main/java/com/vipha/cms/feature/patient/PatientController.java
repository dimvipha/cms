package com.vipha.cms.feature.patient;

import com.vipha.cms.feature.patient.dto.PatientCreateRequest;
import com.vipha.cms.feature.patient.dto.PatientResponse;
import com.vipha.cms.feature.patient.dto.PatientUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientCreateRequest request) {
        return patientService.create(request);
    }

    @GetMapping("/{id}")
    public PatientResponse findOne(@PathVariable UUID id) {
        return patientService.findOne(id);
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody PatientUpdateRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        patientService.delete(id);
    }

    @GetMapping
    public Page<PatientResponse> findAll(
            @RequestParam(required = false, defaultValue = "0") int  pageNumber,
            @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable= PageRequest.of(pageNumber, size, Sort.by("createdAt").ascending());

        return patientService.findAll(pageable);
    }
}
