package com.vipha.cms.feature.prescription;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.pharmacy.MedicineRepository;
import com.vipha.cms.feature.prescription.dto.CreatePrescriptionRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionItemRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.feature.prescription.dto.UpdatePrescriptionRequest;
import com.vipha.cms.feature.prescription.util.PrescriptionSpecs;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitService;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.security.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final VisitService visitService;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionPdfRenderer pdfRenderer;
    private final KeycloakUserService keycloakUserService;
    private final ClinicProperties clinicProperties;

    @Override
    public PrescriptionResponse create(CreatePrescriptionRequest request) {
        Visit visit = visitService.getEditableEntity(request.visitId());
        if (prescriptionRepository.existsByVisitId(visit.getId())) {
            throw new BusinessRuleException("This visit already has a prescription; update it instead");
        }

        Prescription prescription = new Prescription();
        prescription.setVisit(visit);
        prescription.setPatient(visit.getPatient());
        prescription.setDoctor(visit.getDoctor());
        prescription.setPrescribedAt(Instant.now());
        prescription.setNotes(request.notes());
        replaceItems(prescription, request.items());

        return prescriptionMapper.toResponse(prescriptionRepository.save(prescription));
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse get(UUID id) {
        return prescriptionMapper.toResponse(getPrescription(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> findAll(UUID patientId, UUID visitId, int pageNumber, int pageSize) {
        Specification<Prescription> spec = Specification.allOf(
                PrescriptionSpecs.patient(patientId),
                PrescriptionSpecs.visit(visitId));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "prescribedAt"));
        return prescriptionRepository.findAll(spec, pageable).map(prescriptionMapper::toResponse);
    }

    @Override
    public PrescriptionResponse update(UUID id, UpdatePrescriptionRequest request) {
        Prescription prescription = getPrescription(id);
        visitService.getEditableEntity(prescription.getVisit().getId());

        prescription.setNotes(request.notes());
        replaceItems(prescription, request.items());
        // assign ids to the new items before they are mapped
        prescriptionRepository.flush();
        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    public void delete(UUID id) {
        Prescription prescription = getPrescription(id);
        visitService.getEditableEntity(prescription.getVisit().getId());
        prescriptionRepository.delete(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public PdfFile renderPdf(UUID id) {
        Prescription prescription = getPrescription(id);
        if (prescription.getVisit().getStatus() == VisitStatus.CANCELLED) {
            throw new BusinessRuleException("Prescriptions of cancelled visits cannot be printed");
        }

        String doctorName = keycloakUserService.findFullName(prescription.getDoctor().getUserId()).orElse(null);
        byte[] content = pdfRenderer.render(prescription, doctorName);

        LocalDate date = LocalDate.ofInstant(prescription.getPrescribedAt(), clinicProperties.timeZone());
        String fileName = "prescription-%s-%s.pdf".formatted(prescription.getPatient().getMrn(), date);
        return new PdfFile(fileName, content);
    }

    private Prescription getPrescription(UUID id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
    }

    private void replaceItems(Prescription prescription, List<PrescriptionItemRequest> requests) {
        Map<UUID, Medicine> medicines = loadMedicines(requests);

        prescription.getItems().clear();
        for (PrescriptionItemRequest request : requests) {
            PrescriptionItem item = prescriptionMapper.toItem(request);
            item.setMedicine(medicines.get(request.medicineId()));
            item.setPrescription(prescription);
            prescription.getItems().add(item);
        }
    }

    private Map<UUID, Medicine> loadMedicines(List<PrescriptionItemRequest> requests) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (PrescriptionItemRequest request : requests) {
            if (!ids.add(request.medicineId())) {
                throw new BusinessRuleException("Each medicine can appear only once in a prescription");
            }
        }

        // discontinued (inactive) medicines count as missing
        Map<UUID, Medicine> found = medicineRepository.findAllById(ids).stream()
                .filter(Medicine::isActive)
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));
        List<UUID> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Medicine not found or discontinued: " + missing);
        }
        return found;
    }
}
