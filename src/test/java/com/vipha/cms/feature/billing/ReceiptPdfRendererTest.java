package com.vipha.cms.feature.billing;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.feature.visit.VisitStatus;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;

import static com.vipha.cms.feature.billing.BillingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptPdfRendererTest {

    private final ReceiptPdfRenderer renderer = new ReceiptPdfRenderer(
            new ClinicProperties("Sunrise Clinic", "12 Sukhumvit Rd, Bangkok", "02-123-4567", ZoneId.of("Asia/Bangkok")),
            new BillingProperties("THB", new BigDecimal("250.00")));

    @Test
    void paidInvoicePrintsAsReceiptWithPayments() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.DRAFT,
                "Consultation fee", 1, "250.00",
                "Ibuprofen 400mg", 15, "8.00",
                "X-Ray Lumbar Spine", 1, "1500.00");
        invoice.setVisit(visit(VisitStatus.COMPLETED));
        invoice.setTax(new BigDecimal("12.00"));
        invoice.setDiscount(new BigDecimal("50.00"));
        invoice.recalculateTotals();
        payment(invoice, "1000.00", PaymentMethod.CASH, "cashier-1");
        payment(invoice, "832.00", PaymentMethod.BANK_TRANSFER, "cashier-unknown").setReference("TRX-55120");

        String text = textOf(renderer.render(invoice, Map.of("cashier-1", "Usa Thongdee")));

        assertThat(text).contains(
                "Sunrise Clinic", "RECEIPT", "No. INV-2026-00007", "Issued 07 Oct 2026",
                "Pimchanok Rattana", "MRN-000006", "0812345606", "07 Oct 2026, 09:30",
                "Consultation fee", "Ibuprofen 400mg", "120.00", "1,500.00",
                "Subtotal", "1,870.00", "Tax", "12.00", "Discount", "-50.00",
                "Total (THB)", "1,832.00",
                "Payments received", "Cash", "Bank transfer", "TRX-55120", "Usa Thongdee",
                "PAID IN FULL");
        assertThat(text).doesNotContain("INVOICE", "Balance due: THB");
    }

    @Test
    void unpaidInvoicePrintsAsInvoiceWithBalanceDue() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, "Consultation fee", 1, "822.00");
        invoice.refreshPaymentStatus();

        String text = textOf(renderer.render(invoice, Map.of()));

        assertThat(text).contains("INVOICE", "Balance due: THB 822.00", "Visit date");
        assertThat(text).doesNotContain("RECEIPT", "Payments received", "PAID IN FULL", "Tax", "Discount");
    }

    private static String textOf(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
