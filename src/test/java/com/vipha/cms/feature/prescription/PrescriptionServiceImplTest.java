package com.vipha.cms.feature.prescription;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.billing.InvoiceRepository;
import com.vipha.cms.feature.billing.InvoiceStatus;
import com.vipha.cms.feature.care.CareScope;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.pharmacy.MedicineRepository;
import com.vipha.cms.feature.prescription.dto.CreatePrescriptionRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionItemRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.feature.prescription.dto.UpdatePrescriptionRequest;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitService;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.security.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.vipha.cms.security.UserNames;
import org.springframework.test.util.ReflectionTestUtils;

import static com.vipha.cms.feature.prescription.PrescriptionFixtures.medicine;
import static com.vipha.cms.feature.prescription.PrescriptionFixtures.prescription;
import static com.vipha.cms.feature.prescription.PrescriptionFixtures.visit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock UserNames userNames;

    @Mock PrescriptionRepository prescriptionRepository;
    @Mock MedicineRepository medicineRepository;
    @Mock VisitService visitService;
    @Mock PrescriptionPdfRenderer pdfRenderer;
    @Mock KeycloakUserService keycloakUserService;
    @Mock CareScope careScope;
    @Mock InvoiceRepository invoiceRepository;

    PrescriptionServiceImpl service;

    final Medicine loratadine = medicine("MED-005", "Loratadine 10mg");
    final Medicine cetirizine = medicine("MED-009", "Cetirizine 10mg");

    @BeforeEach
    void setUp() {
        PrescriptionMapperImpl mapper = new PrescriptionMapperImpl();
        ReflectionTestUtils.setField(mapper, "userNames", userNames);
        service = new PrescriptionServiceImpl(prescriptionRepository, medicineRepository, visitService,
                mapper, pdfRenderer, keycloakUserService,
                new ClinicProperties("Test Clinic", "", "", ZoneId.of("Asia/Bangkok")), careScope, invoiceRepository);
    }

    @Test
    void create_copiesPatientAndDoctorFromVisit_andKeepsItemOrder() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(cetirizine, loratadine));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = service.create(new CreatePrescriptionRequest(visit.getId(), "Rest",
                List.of(item(loratadine.getId()), item(cetirizine.getId()))));

        assertThat(response.visitId()).isEqualTo(visit.getId());
        assertThat(response.patientId()).isEqualTo(visit.getPatient().getId());
        assertThat(response.doctorId()).isEqualTo(PrescriptionFixtures.DOCTOR_ID);
        assertThat(response.prescribedAt()).isNotNull();
        assertThat(response.items())
                .extracting(i -> i.medicineName() + " / " + i.unit())
                .containsExactly("Loratadine 10mg / tablet", "Cetirizine 10mg / tablet");
    }

    @Test
    void create_rejectsSecondPrescriptionForVisit() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(prescriptionRepository.existsByVisitId(visit.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreatePrescriptionRequest(visit.getId(), null, List.of(item(loratadine.getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has a prescription");
        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void create_rejectsSameMedicineTwice() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);

        assertThatThrownBy(() -> service.create(new CreatePrescriptionRequest(visit.getId(), null,
                List.of(item(loratadine.getId()), item(loratadine.getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only once");
    }

    @Test
    void create_rejectsUnknownOrDiscontinuedMedicine() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        Medicine discontinued = medicine("MED-404", "Old drug");
        discontinued.setActive(false);
        UUID unknown = UUID.randomUUID();
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(loratadine, discontinued));

        assertThatThrownBy(() -> service.create(new CreatePrescriptionRequest(visit.getId(), null,
                List.of(item(loratadine.getId()), item(discontinued.getId()), item(unknown)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(discontinued.getId().toString())
                .hasMessageContaining(unknown.toString())
                .hasMessageNotContaining(loratadine.getId().toString());
    }

    @Test
    void create_onVisitTheCallerCannotEdit_propagatesTheGuard() {
        UUID visitId = UUID.randomUUID();
        when(visitService.getEditableEntity(visitId)).thenThrow(new AccessDeniedException("nope"));

        assertThatThrownBy(() -> service.create(
                new CreatePrescriptionRequest(visitId, null, List.of(item(loratadine.getId())))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_replacesNotesAndItems() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(cetirizine));

        PrescriptionResponse response = service.update(prescription.getId(),
                new UpdatePrescriptionRequest("Switch to cetirizine", List.of(item(cetirizine.getId()))));

        assertThat(response.notes()).isEqualTo("Switch to cetirizine");
        assertThat(response.items()).extracting(i -> i.medicineId()).containsExactly(cetirizine.getId());
        verify(prescriptionRepository).flush();
    }

    @Test
    void update_correctsACompletedVisitThatIsNotBilledYet() {
        Visit visit = visit(VisitStatus.COMPLETED);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(cetirizine));

        PrescriptionResponse response = service.update(prescription.getId(),
                new UpdatePrescriptionRequest(null, List.of(item(cetirizine.getId()))));

        assertThat(response.items()).extracting(i -> i.medicineId()).containsExactly(cetirizine.getId());
    }

    @Test
    void aBilledVisitsPrescriptionStaysAsTheBillHasIt() {
        Visit visit = visit(VisitStatus.COMPLETED);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(visitService.getEditableEntity(visit.getId())).thenReturn(visit);
        when(invoiceRepository.existsByVisitIdAndStatusNot(visit.getId(), InvoiceStatus.VOID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(prescription.getId(),
                new UpdatePrescriptionRequest(null, List.of(item(cetirizine.getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("billed");
        assertThatThrownBy(() -> service.delete(prescription.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("billed");
        assertThatThrownBy(() -> service.create(
                new CreatePrescriptionRequest(visit.getId(), null, List.of(item(cetirizine.getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("billed");
        verify(prescriptionRepository, never()).delete(any(Prescription.class));
    }

    @Test
    void delete_checksTheVisitIsStillEditable() {
        Visit visit = visit(VisitStatus.CANCELLED);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(visitService.getEditableEntity(visit.getId()))
                .thenThrow(new BusinessRuleException("Visit is CANCELLED and can no longer be changed"));

        assertThatThrownBy(() -> service.delete(prescription.getId()))
                .isInstanceOf(BusinessRuleException.class);
        verify(prescriptionRepository, never()).delete(any(Prescription.class));
    }

    @Test
    void renderPdf_namesFileAfterMrnAndLocalDate() {
        Visit visit = visit(VisitStatus.COMPLETED);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(keycloakUserService.findFullName(PrescriptionFixtures.DOCTOR_ID)).thenReturn(Optional.of("Malee Siriwan"));
        when(pdfRenderer.render(prescription, "Malee Siriwan")).thenReturn(new byte[]{1, 2, 3});

        PdfFile pdf = service.renderPdf(prescription.getId());

        // prescribed 2026-10-07T03:00Z = 10:00 in Bangkok
        assertThat(pdf.fileName()).isEqualTo("prescription-MRN-000002-2026-10-07.pdf");
        assertThat(pdf.content()).containsExactly(1, 2, 3);
    }

    @Test
    void renderPdf_stillWorksWhenDoctorNameIsUnavailable() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(keycloakUserService.findFullName(any())).thenReturn(Optional.empty());
        when(pdfRenderer.render(eq(prescription), eq(null))).thenReturn(new byte[]{1});

        assertThat(service.renderPdf(prescription.getId()).content()).hasSize(1);
    }

    @Test
    void renderPdf_rejectsCancelledVisit() {
        Visit visit = visit(VisitStatus.CANCELLED);
        Prescription prescription = prescription(visit, loratadine);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.renderPdf(prescription.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    private static PrescriptionItemRequest item(UUID medicineId) {
        return new PrescriptionItemRequest(medicineId, "10mg", "Once daily", "7 days", 7, null);
    }
}
