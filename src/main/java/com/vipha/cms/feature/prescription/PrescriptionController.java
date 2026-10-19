package com.vipha.cms.feature.prescription;

import com.vipha.cms.feature.prescription.dto.CreatePrescriptionRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.feature.prescription.dto.UpdatePrescriptionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return prescriptionService.create(request);
    }

    /** Filter by {@code visitId} (the doctor's screen) or {@code patientId} (medication history). */
    @GetMapping
    public Page<PrescriptionResponse> findAll(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return prescriptionService.findAll(patientId, visitId, pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse get(@PathVariable UUID id) {
        return prescriptionService.get(id);
    }

    @PutMapping("/{id}")
    public PrescriptionResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdatePrescriptionRequest request) {
        return prescriptionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        prescriptionService.delete(id);
    }

    /** Printable prescription; opens inline in the browser's PDF viewer. */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        return prescriptionService.renderPdf(id).toInlineResponse();
    }
}
