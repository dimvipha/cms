package com.vipha.cms.feature.visit;

import com.vipha.cms.feature.visit.dto.CreateVisitRequest;
import com.vipha.cms.feature.visit.dto.RecordVitalSignRequest;
import com.vipha.cms.feature.visit.dto.UpdateVisitRequest;
import com.vipha.cms.feature.visit.dto.VisitResponse;
import com.vipha.cms.feature.visit.dto.VisitSummaryResponse;
import com.vipha.cms.feature.visit.dto.VitalSignResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface VisitService {

    VisitResponse create(CreateVisitRequest request);

    VisitResponse get(UUID id);

    Page<VisitSummaryResponse> findAll(UUID patientId, String doctorId, VisitStatus status, LocalDate date,
                                       int pageNumber, int pageSize);

    Page<VisitSummaryResponse> findMine(VisitStatus status, LocalDate date, int pageNumber, int pageSize);

    VisitResponse update(UUID id, UpdateVisitRequest request);

    VisitResponse start(UUID id);

    VisitResponse complete(UUID id);

    VisitResponse cancel(UUID id);

    VitalSignResponse recordVitals(UUID visitId, RecordVitalSignRequest request);

    /** The visit, provided it is still open and the caller is its doctor (or an admin). */
    Visit getEditableEntity(UUID id);
}
