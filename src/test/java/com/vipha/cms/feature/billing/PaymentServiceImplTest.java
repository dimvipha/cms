package com.vipha.cms.feature.billing;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.feature.billing.dto.InvoiceResponse;
import com.vipha.cms.feature.billing.dto.RecordPaymentRequest;
import com.vipha.cms.feature.patient.PatientMapperImpl;
import com.vipha.cms.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import com.vipha.cms.security.UserNames;

import static com.vipha.cms.feature.billing.BillingFixtures.invoice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock UserNames userNames;

    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;

    PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        InvoiceMapperImpl mapper = new InvoiceMapperImpl();
        ReflectionTestUtils.setField(mapper, "patientMapper", new PatientMapperImpl());
        ReflectionTestUtils.setField(mapper, "userNames", userNames);
        service = new PaymentServiceImpl(invoiceRepository, paymentRepository, mapper);
        TestAuth.loginAs("cashier-1", "RECEPTIONIST");
    }

    @AfterEach
    void tearDown() {
        TestAuth.logout();
    }

    @Test
    void partPaymentThenSettlement() {
        Invoice invoice = issued("822.00");
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse afterFirst = service.record(invoice.getId(), pay("500", PaymentMethod.CASH, null));
        assertThat(afterFirst.status()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(afterFirst.amountPaid()).isEqualByComparingTo("500.00");
        assertThat(afterFirst.balance()).isEqualByComparingTo("322.00");

        InvoiceResponse afterSecond = service.record(invoice.getId(), pay("322.00", PaymentMethod.CARD, "AUTH-7781"));
        assertThat(afterSecond.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(afterSecond.balance()).isEqualByComparingTo("0.00");
        assertThat(afterSecond.payments())
                .extracting(p -> p.method() + " " + p.amount() + " " + p.reference())
                .containsExactly("CASH 500.00 null", "CARD 322.00 AUTH-7781");
    }

    @Test
    void recordsWhoTookThePaymentAndWhen() {
        Invoice invoice = issued("250.00");
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse response = service.record(invoice.getId(), pay("250", PaymentMethod.MOBILE_PAYMENT, "PP-001"));

        assertThat(response.payments()).singleElement().satisfies(p -> {
            assertThat(p.receivedBy()).isEqualTo("cashier-1");
            assertThat(p.paidAt()).isNotNull();
            assertThat(p.amount()).isEqualTo(new BigDecimal("250.00"));
        });
    }

    @Test
    void rejectsPaymentAboveTheBalance() {
        Invoice invoice = issued("822.00");

        assertThatThrownBy(() -> service.record(invoice.getId(), pay("822.01", PaymentMethod.CASH, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("more than the balance of 822.00");
        verify(paymentRepository, never()).save(any());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = {"DRAFT", "PAID", "VOID"})
    void rejectsPaymentOnInvoicesThatCannotTakeOne(InvoiceStatus status) {
        Invoice invoice = invoice(status, "Consultation fee", 1, "250.00");
        when(invoiceRepository.findForUpdateById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.record(invoice.getId(), pay("1", PaymentMethod.CASH, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(paymentRepository, never()).save(any());
    }

    private Invoice issued(String total) {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Visit charges", 1, total);
        invoice.refreshPaymentStatus(); // as issue() does
        when(invoiceRepository.findForUpdateById(invoice.getId())).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private static RecordPaymentRequest pay(String amount, PaymentMethod method, String reference) {
        return new RecordPaymentRequest(new BigDecimal(amount), method, reference);
    }
}
