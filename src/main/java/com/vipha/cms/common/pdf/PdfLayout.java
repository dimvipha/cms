package com.vipha.cms.common.pdf;

import com.vipha.cms.common.ClinicProperties;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared look for printed documents (prescriptions, receipts): A4 page, clinic header,
 * label/value fields, ruled item tables and a footer. Uses the PDF base-14 fonts, so
 * text must be Latin-1 (cp1252).
 */
public final class PdfLayout {

    public static final Color INK = new Color(33, 33, 33);
    public static final Color MUTED = new Color(110, 110, 110);
    public static final Color RULE = new Color(200, 200, 200);
    public static final Color HEADER_FILL = new Color(240, 240, 240);
    public static final Color ALERT = new Color(176, 0, 32);
    public static final Color SUCCESS = new Color(27, 120, 55);

    public static final Font CLINIC = new Font(Font.HELVETICA, 16, Font.BOLD, INK);
    public static final Font TITLE = new Font(Font.HELVETICA, 13, Font.BOLD, INK);
    public static final Font SECTION = new Font(Font.HELVETICA, 10, Font.BOLD, INK);
    public static final Font TEXT = new Font(Font.HELVETICA, 10, Font.NORMAL, INK);
    public static final Font TEXT_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD, INK);
    public static final Font ALERT_TEXT = new Font(Font.HELVETICA, 10, Font.BOLD, ALERT);
    public static final Font SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private PdfLayout() {}

    /** Opens an A4 document writing to {@code out}, with {@code footer} and a page number on every page. */
    public static Document open(OutputStream out, String title, String creator, String footer) {
        Document document = new Document(PageSize.A4, 42, 42, 42, 60);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new Footer(footer));
        document.addTitle(title);
        document.addCreator(creator);
        document.open();
        return document;
    }

    /** Clinic name and contact on the left, document title and reference lines on the right, then a rule. */
    public static void addHeader(Document document, ClinicProperties clinic, String title, String... references) {
        PdfPTable table = new PdfPTable(new float[]{3, 2});
        table.setWidthPercentage(100);

        PdfPCell clinicCell = borderless();
        clinicCell.addElement(new Paragraph(clinic.name(), CLINIC));
        String contact = joinNonBlank("  |  ", clinic.address(), clinic.phone());
        if (!contact.isEmpty()) {
            clinicCell.addElement(new Paragraph(contact, SMALL));
        }
        table.addCell(clinicCell);

        PdfPCell titleCell = borderless();
        titleCell.addElement(rightAligned(title, TITLE));
        for (String reference : references) {
            titleCell.addElement(rightAligned(reference, SMALL));
        }
        table.addCell(titleCell);

        document.add(table);
        document.add(rule());
    }

    /** A full-width table whose first row is a shaded header that repeats on page breaks. */
    public static PdfPTable itemTable(float[] widths, String... headings) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setHeaderRows(1);
        for (String heading : headings) {
            PdfPCell cell = itemCell(heading, SECTION);
            cell.setBackgroundColor(HEADER_FILL);
            table.addCell(cell);
        }
        return table;
    }

    /** A signature line at the bottom right, with an optional name above the caption. */
    public static void addSignature(Document document, String name, String caption) {
        PdfPTable table = new PdfPTable(new float[]{3, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(48);
        table.setKeepTogether(true);

        table.addCell(borderless());

        PdfPCell signature = borderless();
        signature.setBorder(Rectangle.TOP);
        signature.setBorderColor(INK);
        if (name != null) {
            signature.addElement(centered(name, TEXT_BOLD));
        }
        signature.addElement(centered(caption, SMALL));
        table.addCell(signature);

        document.add(table);
    }

    public static void addField(PdfPTable table, String label, String value, Font font) {
        table.addCell(labelCell(label));
        table.addCell(valueCell(value, font));
    }

    public static PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SMALL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(5);
        return cell;
    }

    public static PdfPCell valueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(3);
        return cell;
    }

    public static PdfPCell itemCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(RULE);
        cell.setPadding(5);
        return cell;
    }

    public static PdfPCell borderless() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    public static Paragraph section(String title) {
        Paragraph paragraph = new Paragraph(title, SECTION);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(2);
        return paragraph;
    }

    public static Paragraph rule() {
        Paragraph paragraph = new Paragraph(new Chunk(new LineSeparator(0.6f, 100, RULE, Element.ALIGN_CENTER, -2)));
        paragraph.setSpacingAfter(4);
        return paragraph;
    }

    public static Paragraph rightAligned(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }

    public static Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    public static String joinNonBlank(String separator, String... parts) {
        return Stream.of(parts).filter(StringUtils::hasText).collect(Collectors.joining(separator));
    }

    /** "BANK_TRANSFER" → "Bank transfer". */
    public static String humanize(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        String spaced = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    public static String orDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Footer extends PdfPageEventHelper {

        private final String text;

        private Footer(String text) {
            this.text = text;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 28;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(text, SMALL), document.left(), y, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), SMALL), document.right(), y, 0);
        }
    }
}
