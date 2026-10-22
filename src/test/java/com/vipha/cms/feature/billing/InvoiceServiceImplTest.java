package com.vipha.cms.feature.billing;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.common.dto.PdfFile;
import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.billing.dto.CreateInvoiceRequest;
import com.vipha.cms.feature.billing.dto.InvoiceItemRequest;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.UpdateInvoiceRequest;
import com.vipha.cms.feature.laboratory.LabOrderRepository;
import com.vipha.cms.feature.laboratory.LabOrderStatus;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.patient.PatientMapperImpl;
import com.vipha.cms.feature.patient.PatientRepository;
import com.vipha.cms.feature.prescription.PrescriptionRepository;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitRepository;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.security.KeycloakUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.vipha.cms.security.UserNames;

import static com.vipha.cms.feature.billing.BillingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    @Mock UserNames userNames;

    @Mock InvoiceRepository invoiceRepository;
    @Mock VisitRepository visitRepository;
    @Mock PatientRepository patientRepository;
    @Mock PrescriptionRepository prescriptionRepository;
    @Mock LabOrderRepository labOrderRepository;
    @Mock ReceiptPdfRenderer receiptRenderer;
    @Mock KeycloakUserService keycloakUserService;

    InvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        InvoiceMapperImpl mapper = new InvoiceMapperImpl();
        ReflectionTestUtils.setField(mapper, "patientMapper", new PatientMapperImpl());
        ReflectionTestUtils.setField(mapper, "userNames", userNames);
        service = new InvoiceServiceImpl(invoiceRepository, visitRepository, patientRepository,
                prescriptionRepository, labOrderRepository, mapper, receiptRenderer, keycloakUserService,
                new BillingProperties("THB", new BigDecimal("250.00")),
                new ClinicProperties("Test Clinic", "", "", BANGKOK));
    }

    // ── from visit ──────────────────────────────────────────────────────────

    @Test
    void createFromVisit_billsConsultationPrescriptionAndLabTests() {
        Visit visit = completedVisit();
        var amoxicillin = medicine("Amoxicillin 500mg", "12.00");
        var paracetamol = medicine("Paracetamol 500mg", "5.00");
        when(prescriptionRepository.findByVisitId(visit.getId()))
                .thenReturn(Optional.of(prescription(visit, amoxicillin, 15, paracetamol, 10)));
        when(labOrderRepository.findByVisitId(visit.getId())).thenReturn(List.of(
                labOrder(visit, LabOrderStatus.COMPLETED,
                        labItem("Complete Blood Count (CBC)", "250.00", LabOrderStatus.COMPLETED),
                        labItem("Lipid Panel", "350.00", LabOrderStatus.CANCELLED)),
                labOrder(visit, LabOrderStatus.CANCELLED,
                        labItem("Chest X-Ray", "500.00", LabOrderStatus.PENDING))));
        when(invoiceRepository.nextInvoiceSequence()).thenReturn(42L);
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse invoice = service.createFromVisit(visit.getId());

        assertThat(invoice.invoiceNumber()).isEqualTo("INV-" + Year.now(BANGKOK) + "-00042");
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.visitId()).isEqualTo(visit.getId());
        assertThat(invoice.patient().mrn()).isEqualTo("MRN-000006");
        assertThat(invoice.items())
                .extracting(i -> i.itemType() + " " + i.description() + " " + i.quantity() + " x " + i.unitPrice() + " = " + i.subtotal())
                .containsExactly(
                        "CONSULTATION Consultation fee 1 x 250.00 = 250.00",
                        "MEDICINE Amoxicillin 500mg 15 x 12.00 = 180.00",
                        "MEDICINE Paracetamol 500mg 10 x 5.00 = 50.00",
                        "LAB_TEST Complete Blood Count (CBC) 1 x 250.00 = 250.00");
        assertThat(invoice.items()).allSatisfy(i -> assertThat(i.refId()).isNotNull());
        assertThat(invoice.subtotal()).isEqualByComparingTo("730.00");
        assertThat(invoice.total()).isEqualByComparingTo("730.00");
        assertThat(invoice.balance()).isEqualByComparingTo("730.00");
    }

    @Test
    void createFromVisit_withoutPrescriptionOrLabs_billsConsultationOnly() {
        Visit visit = completedVisit();
        when(prescriptionRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
        when(labOrderRepository.findByVisitId(visit.getId())).thenReturn(List.of());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse invoice = service.createFromVisit(visit.getId());

        assertThat(invoice.items()).hasSize(1);
        assertThat(invoice.total()).isEqualByComparingTo("250.00");
    }

    @Test
    void createFromVisit_requiresCompletedVisit() {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.createFromVisit(visit.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only completed visits");
    }

    @Test
    void createFromVisit_rejectsVisitThatIsAlreadyBilled() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(invoiceRepository.existsByVisitIdAndStatusNot(visit.getId(), InvoiceStatus.VOID)).thenReturn(true);

        assertThatThrownBy(() -> service.createFromVisit(visit.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has an invoice");
        verify(invoiceRepository, never()).save(any());
    }

    // ── manual invoice / draft editing ──────────────────────────────────────

    @Test
    void create_appliesTaxAndDiscount() {
        Patient patient = patient();
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse invoice = service.create(new CreateInvoiceRequest(patient.getId(), null,
                new BigDecimal("10"), new BigDecimal("60"),
                List.of(line("Dressing", 2, "100"), line("Bandage", 1, "50"))));

        assertThat(invoice.subtotal()).isEqualByComparingTo("250.00");
        assertThat(invoice.tax()).isEqualByComparingTo("10.00");
        assertThat(invoice.discount()).isEqualByComparingTo("60.00");
        assertThat(invoice.total()).isEqualByComparingTo("200.00");
        assertThat(invoice.visitId()).isNull();
    }

    @Test
    void create_rejectsDiscountLargerThanTheBill() {
        Patient patient = patient();
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.create(new CreateInvoiceRequest(patient.getId(), null,
                null, new BigDecimal("101"), List.of(line("Dressing", 1, "100")))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Discount");
    }

    @Test
    void create_rejectsVisitOfAnotherPatient() {
        Patient patient = patient();
        Visit visit = completedVisit(); // has its own patient
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.create(new CreateInvoiceRequest(patient.getId(), visit.getId(),
                null, null, List.of(line("Dressing", 1, "100")))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different patient");
    }

    @Test
    void update_replacesLinesOfDraft() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse updated = service.update(invoice.getId(), new UpdateInvoiceRequest(null, new BigDecimal("50"),
                List.of(line("Consultation fee", 1, "250"), line("X-Ray Lumbar Spine", 1, "500"))));

        assertThat(updated.items()).extracting(i -> i.description())
                .containsExactly("Consultation fee", "X-Ray Lumbar Spine");
        assertThat(updated.total()).isEqualByComparingTo("700.00");
        verify(invoiceRepository).flush();
    }

    @Test
    void update_rejectsIssuedInvoice() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.update(invoice.getId(),
                new UpdateInvoiceRequest(null, null, List.of(line("x", 1, "1")))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only draft");
    }

    // ── issue / void ────────────────────────────────────────────────────────

    @Test
    void issue_makesDraftPayable() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse issued = service.issue(invoice.getId());

        assertThat(issued.status()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(issued.balance()).isEqualByComparingTo("250.00");
    }

    @Test
    void issue_zeroTotalInvoiceIsPaidStraightAway() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Follow-up (no charge)", 1, "0.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThat(service.issue(invoice.getId()).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void issue_twiceIsRejected() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.issue(invoice.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void void_unpaidInvoice() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findForUpdateById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThat(service.voidInvoice(invoice.getId()).status()).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void void_rejectsInvoiceWithPayments() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        payment(invoice, "100.00", PaymentMethod.CASH, "cashier-1");
        when(invoiceRepository.findForUpdateById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.voidInvoice(invoice.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("payments");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    // ── printing ────────────────────────────────────────────────────────────

    @Test
    void renderPdf_rejectsDraft() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.renderPdf(invoice.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Issue the invoice");
    }

    @Test
    void renderPdf_namesReceiptAndResolvesCashierNames() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        payment(invoice, "100.00", PaymentMethod.CASH, "cashier-1");
        payment(invoice, "150.00", PaymentMethod.CARD, "cashier-gone");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(keycloakUserService.findFullName("cashier-1")).thenReturn(Optional.of("Usa Thongdee"));
        when(keycloakUserService.findFullName("cashier-gone")).thenReturn(Optional.empty());
        when(receiptRenderer.render(invoice, Map.of("cashier-1", "Usa Thongdee"))).thenReturn(new byte[]{1});

        PdfFile pdf = service.renderPdf(invoice.getId());

        assertThat(pdf.fileName()).isEqualTo("receipt-INV-2026-00007.pdf");
    }

    @Test
    void renderPdf_unpaidInvoiceIsNamedInvoice() {
        Invoice invoice = invoice(InvoiceStatus.UNPAID, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(receiptRenderer.render(invoice, Map.of())).thenReturn(new byte[]{1});

        assertThat(service.renderPdf(invoice.getId()).fileName()).isEqualTo("invoice-INV-2026-00007.pdf");
    }

    private Visit completedVisit() {
        Visit visit = visit(VisitStatus.COMPLETED);
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        return visit;
    }

    private static InvoiceItemRequest line(String description, int quantity, String unitPrice) {
        return new InvoiceItemRequest(ItemType.PROCEDURE, null, description, quantity, new BigDecimal(unitPrice));
    }
}
