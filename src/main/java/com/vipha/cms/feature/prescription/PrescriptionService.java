package com.vipha.cms.feature.prescription;

import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.feature.prescription.dto.CreatePrescriptionRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.feature.prescription.dto.UpdatePrescriptionRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PrescriptionService {

    PrescriptionResponse create(CreatePrescriptionRequest request);

    PrescriptionResponse get(UUID id);

    Page<PrescriptionResponse> findAll(UUID patientId, UUID visitId, int pageNumber, int pageSize);

    PrescriptionResponse update(UUID id, UpdatePrescriptionRequest request);

    void delete(UUID id);

    PdfFile renderPdf(UUID id);
}
