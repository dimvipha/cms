package com.vipha.cms.feature.visit;

import com.vipha.cms.feature.visit.dto.CreateVisitRequest;
import com.vipha.cms.feature.visit.dto.RecordVitalSignRequest;
import com.vipha.cms.feature.visit.dto.UpdateVisitRequest;
import com.vipha.cms.feature.visit.dto.VisitResponse;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import com.vipha.cms.feature.visit.dto.VitalSignResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse create(@Valid @RequestBody CreateVisitRequest request) {
        return visitService.create(request);
    }

    @GetMapping
    public Page<VisitSummaryResponse> findAll(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return visitService.findAll(patientId, doctorId, status, date, pageNumber, pageSize);
    }

    /** The calling doctor's queue, e.g. {@code ?date=2026-10-07&status=OPEN}. */
    @GetMapping("/me")
    public Page<VisitSummaryResponse> findMine(
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return visitService.findMine(status, date, pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public VisitResponse get(@PathVariable UUID id) {
        return visitService.get(id);
    }

    @PatchMapping("/{id}")
    public VisitResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVisitRequest request) {
        return visitService.update(id, request);
    }

    @PostMapping("/{id}/start")
    public VisitResponse start(@PathVariable UUID id) {
        return visitService.start(id);
    }

    @PostMapping("/{id}/complete")
    public VisitResponse complete(@PathVariable UUID id) {
        return visitService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public VisitResponse cancel(@PathVariable UUID id) {
        return visitService.cancel(id);
    }

    @PostMapping("/{id}/vitals")
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse recordVitals(@PathVariable UUID id,
                                          @Valid @RequestBody RecordVitalSignRequest request) {
        return visitService.recordVitals(id, request);
    }
}
