package com.vipha.cms.feature.prescription;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VitalSign;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.vipha.cms.common.pdf.PdfLayout.*;

/**
 * Renders a prescription as an A4 PDF: clinic header, patient details, the latest
 * vitals and diagnosis from the visit, the Rx table, advice notes and a signature line.
 */
@Component
@RequiredArgsConstructor
public class PrescriptionPdfRenderer {

    private static final Font RX = new Font(Font.HELVETICA, 20, Font.BOLDITALIC, INK);

    private final ClinicProperties clinic;

    /**
     * @param doctorName the prescriber's display name, or null when it could not be resolved
     */
    public byte[] render(Prescription prescription, String doctorName) {
        ZoneId zone = clinic.timeZone();
        String rxNumber = prescription.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(out, "Prescription " + rxNumber, clinic.name(),
                "Printed " + DATE_TIME.format(Instant.now().atZone(zone))
                        + "   |   Prescription " + prescription.getId());

        addHeader(document, clinic, "PRESCRIPTION",
                "Rx No. " + rxNumber,
                DATE.format(prescription.getPrescribedAt().atZone(zone)));
        addPatient(document, prescription, doctorName != null ? "Dr. " + doctorName : "-", zone);
        addClinicalSummary(document, prescription.getVisit(), zone);
        addItems(document, prescription.getItems());
        addNotes(document, prescription.getNotes());
        addSignature(document, doctorName != null ? "Dr. " + doctorName : null, "Signature & stamp");

        document.close();
        return out.toByteArray();
    }

    private void addPatient(Document document, Prescription prescription, String doctor, ZoneId zone) {
        Patient patient = prescription.getPatient();
        LocalDate prescribedOn = LocalDate.ofInstant(prescription.getPrescribedAt(), zone);
        int age = Period.between(patient.getDob(), prescribedOn).getYears();

        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.4f, 1.1f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addField(table, "Patient", patient.getFirstName() + " " + patient.getLastName(), TEXT_BOLD);
        addField(table, "MRN", patient.getMrn(), TEXT);
        addField(table, "Age / Sex", age + " y / " + humanize(patient.getGender()), TEXT);
        addField(table, "Blood group", orDash(patient.getBloodGroup()), TEXT);
        addField(table, "Doctor", doctor, TEXT);
        addField(table, "Visit date", DATE_TIME.format(prescription.getVisit().getVisitDate().atZone(zone)), TEXT);

        boolean hasAllergies = StringUtils.hasText(patient.getAllergies());
        table.addCell(labelCell("Allergies"));
        PdfPCell allergies = valueCell(hasAllergies ? patient.getAllergies() : "None recorded",
                hasAllergies ? ALERT_TEXT : TEXT);
        allergies.setColspan(3);
        table.addCell(allergies);

        document.add(table);
    }

    private void addClinicalSummary(Document document, Visit visit, ZoneId zone) {
        document.add(section("Vital signs"));
        List<VitalSign> vitals = visit.getVitalSigns();
        if (vitals.isEmpty()) {
            document.add(new Paragraph("Not recorded", SMALL));
        } else {
            VitalSign latest = vitals.getLast();
            document.add(vitalsTable(latest));
            document.add(new Paragraph("Recorded " + DATE_TIME.format(latest.getRecordedAt().atZone(zone)), SMALL));
        }

        if (StringUtils.hasText(visit.getChiefComplaint())) {
            document.add(section("Chief complaint"));
            document.add(new Paragraph(visit.getChiefComplaint(), TEXT));
        }

        document.add(section("Diagnosis"));
        String diagnosis = orDash(visit.getDiagnosis());
        if (StringUtils.hasText(visit.getDiagnosisCode())) {
            diagnosis += "  (ICD-10 " + visit.getDiagnosisCode() + ")";
        }
        document.add(new Paragraph(diagnosis, TEXT));
    }

    private PdfPTable vitalsTable(VitalSign v) {
        List<String[]> readings = new ArrayList<>();
        if (v.getSystolicBp() != null && v.getDiastolicBp() != null) {
            readings.add(new String[]{"Blood pressure", v.getSystolicBp() + "/" + v.getDiastolicBp() + " mmHg"});
        }
        addReading(readings, "Pulse", v.getPulseRate(), "bpm");
        addReading(readings, "Temperature", v.getTemperatureCelsius(), "°C");
        addReading(readings, "Resp. rate", v.getRespiratoryRate(), "/min");
        addReading(readings, "SpO2", v.getOxygenSaturation(), "%");
        addReading(readings, "Weight", v.getWeightKg(), "kg");
        addReading(readings, "Height", v.getHeightCm(), "cm");
        addReading(readings, "BMI", v.getBmi(), "");

        // blood pressure carries the longest value, so give it a wider column
        float[] widths = new float[Math.max(readings.size(), 1)];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = i < readings.size() && readings.get(i)[0].equals("Blood pressure") ? 1.6f : 1f;
        }
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        for (String[] reading : readings) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(RULE);
            cell.setPadding(4);
            cell.addElement(new Paragraph(reading[0], SMALL));
            cell.addElement(new Paragraph(reading[1], TEXT_BOLD));
            table.addCell(cell);
        }
        if (readings.isEmpty()) {
            table.addCell(valueCell("-", TEXT));
        }
        return table;
    }

    private void addItems(Document document, List<PrescriptionItem> items) {
        Paragraph rx = new Paragraph("Rx", RX);
        rx.setSpacingBefore(12);
        document.add(rx);

        PdfPTable table = itemTable(new float[]{0.4f, 2.6f, 1.1f, 1.5f, 1.1f, 1.0f, 2.4f},
                "#", "Medicine", "Dose", "Frequency", "Duration", "Qty", "Instructions");

        int line = 1;
        for (PrescriptionItem item : items) {
            Medicine medicine = item.getMedicine();
            table.addCell(itemCell(String.valueOf(line++), TEXT));
            table.addCell(itemCell(medicine.getName(), TEXT_BOLD));
            table.addCell(itemCell(item.getDosage(), TEXT));
            table.addCell(itemCell(item.getFrequency(), TEXT));
            table.addCell(itemCell(item.getDuration(), TEXT));
            table.addCell(itemCell(item.getQuantity() + " " + medicine.getUnit(), TEXT));
            table.addCell(itemCell(orEmpty(item.getInstructions()), TEXT));
        }

        document.add(table);
    }

    private void addNotes(Document document, String notes) {
        if (!StringUtils.hasText(notes)) {
            return;
        }
        document.add(section("Advice"));
        document.add(new Paragraph(notes, TEXT));
    }

    private static void addReading(List<String[]> readings, String label, Object value, String unit) {
        if (value == null) {
            return;
        }
        String text = value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : value.toString();
        readings.add(new String[]{label, unit.isEmpty() ? text : text + " " + unit});
    }
}
