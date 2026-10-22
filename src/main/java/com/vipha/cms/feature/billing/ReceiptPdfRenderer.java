package com.vipha.cms.feature.billing;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.feature.patient.Patient;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

import static com.vipha.cms.common.pdf.PdfLayout.*;

/**
 * Renders an issued invoice as an A4 PDF. Titled "INVOICE" while nothing has been paid and
 * "RECEIPT" once money has been received; either way it lists the lines, totals, payments
 * so far and the balance still due.
 */
@Component
@RequiredArgsConstructor
public class ReceiptPdfRenderer {

    private static final Font TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD, INK);
    private static final Font BALANCE_DUE = new Font(Font.HELVETICA, 11, Font.BOLD, ALERT);
    private static final Font PAID_IN_FULL = new Font(Font.HELVETICA, 11, Font.BOLD, SUCCESS);

    private final ClinicProperties clinic;
    private final BillingProperties billing;

    /**
     * @param staffNames display names of the staff who took payments, keyed by user id;
     *                   anyone missing is printed as "-"
     */
    public byte[] render(Invoice invoice, Map<String, String> staffNames) {
        ZoneId zone = clinic.timeZone();
        String title = invoice.getStatus() == InvoiceStatus.UNPAID ? "INVOICE" : "RECEIPT";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(out, title + " " + invoice.getInvoiceNumber(), clinic.name(),
                "Printed " + DATE_TIME.format(Instant.now().atZone(zone))
                        + "   |   Invoice " + invoice.getInvoiceNumber());

        addHeader(document, clinic, title,
                "No. " + invoice.getInvoiceNumber(),
                "Issued " + DATE.format(invoice.getIssuedAt().atZone(zone)));
        addBillTo(document, invoice, zone);
        addLines(document, invoice);
        addTotals(document, invoice);
        if (!invoice.getPayments().isEmpty()) {
            addPayments(document, invoice, staffNames, zone);
        }
        addClosing(document, invoice);
        addSignature(document, null, "Authorised signature");

        document.close();
        return out.toByteArray();
    }

    private void addBillTo(Document document, Invoice invoice, ZoneId zone) {
        Patient patient = invoice.getPatient();

        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.4f, 1.1f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addField(table, "Bill to", patient.getFirstName() + " " + patient.getLastName(), TEXT_BOLD);
        addField(table, "MRN", patient.getMrn(), TEXT);
        addField(table, "Phone", orDash(patient.getPhone()), TEXT);
        addField(table, "Visit date", invoice.getVisit() == null ? "-"
                : DATE_TIME.format(invoice.getVisit().getVisitDate().atZone(zone)), TEXT);

        document.add(table);
    }

    private void addLines(Document document, Invoice invoice) {
        document.add(section("Charges"));
        PdfPTable table = itemTable(new float[]{0.4f, 3.4f, 1.3f, 0.7f, 1.3f, 1.4f},
                "#", "Description", "Type", "Qty", "Unit price", "Amount");

        int line = 1;
        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(itemCell(String.valueOf(line++), TEXT));
            table.addCell(itemCell(item.getDescription(), TEXT));
            table.addCell(itemCell(humanize(item.getItemType().name()), TEXT));
            table.addCell(amountCell(String.valueOf(item.getQuantity()), TEXT));
            table.addCell(amountCell(format(item.getUnitPrice()), TEXT));
            table.addCell(amountCell(format(item.getSubtotal()), TEXT));
        }

        document.add(table);
    }

    private void addTotals(Document document, Invoice invoice) {
        PdfPTable totals = new PdfPTable(new float[]{1.6f, 1.4f});
        addTotal(totals, "Subtotal", format(invoice.getSubtotal()), TEXT);
        if (invoice.getTax().signum() > 0) {
            addTotal(totals, "Tax", format(invoice.getTax()), TEXT);
        }
        if (invoice.getDiscount().signum() > 0) {
            addTotal(totals, "Discount", "-" + format(invoice.getDiscount()), TEXT);
        }
        addTotal(totals, "Total (" + billing.currency() + ")", format(invoice.getTotal()), TOTAL);
        addTotal(totals, "Paid", format(invoice.getAmountPaid()), TEXT);
        boolean settled = invoice.getBalance().signum() <= 0;
        addTotal(totals, "Balance due", format(invoice.getBalance()), settled ? TOTAL : BALANCE_DUE);

        // right-hand column only
        PdfPTable layout = new PdfPTable(new float[]{3, 2});
        layout.setWidthPercentage(100);
        layout.setSpacingBefore(6);
        layout.addCell(borderless());
        PdfPCell cell = borderless();
        cell.addElement(totals);
        layout.addCell(cell);
        document.add(layout);
    }

    private void addPayments(Document document, Invoice invoice, Map<String, String> staffNames, ZoneId zone) {
        document.add(section("Payments received"));
        PdfPTable table = itemTable(new float[]{1.6f, 1.3f, 1.6f, 1.6f, 1.3f},
                "Date", "Method", "Reference", "Received by", "Amount");

        for (Payment payment : invoice.getPayments()) {
            table.addCell(itemCell(DATE_TIME.format(payment.getPaidAt().atZone(zone)), TEXT));
            table.addCell(itemCell(humanize(payment.getMethod().name()), TEXT));
            table.addCell(itemCell(orDash(payment.getReference()), TEXT));
            table.addCell(itemCell(orDash(staffNames.get(payment.getReceivedBy())), TEXT));
            table.addCell(amountCell(format(payment.getAmount()), TEXT));
        }

        document.add(table);
    }

    private void addClosing(Document document, Invoice invoice) {
        Paragraph status = invoice.getStatus() == InvoiceStatus.PAID
                ? new Paragraph("PAID IN FULL", PAID_IN_FULL)
                : new Paragraph("Balance due: " + billing.currency() + " " + format(invoice.getBalance()), BALANCE_DUE);
        status.setSpacingBefore(14);
        document.add(status);

        Paragraph thanks = new Paragraph("Thank you for choosing " + clinic.name()
                + ". Please keep this document for your records.", SMALL);
        thanks.setSpacingBefore(4);
        document.add(thanks);
    }

    private static void addTotal(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = valueCell(label, font);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        PdfPCell valueCell = valueCell(value, font);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private static PdfPCell amountCell(String text, Font font) {
        PdfPCell cell = itemCell(text, font);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static String format(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "%,.2f", amount);
    }
}
