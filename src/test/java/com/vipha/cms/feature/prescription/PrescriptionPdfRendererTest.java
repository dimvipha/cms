package com.vipha.cms.feature.prescription;

import com.vipha.cms.common.ClinicProperties;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitStatus;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

import static com.vipha.cms.feature.prescription.PrescriptionFixtures.medicine;
import static com.vipha.cms.feature.prescription.PrescriptionFixtures.prescription;
import static com.vipha.cms.feature.prescription.PrescriptionFixtures.visit;
import static com.vipha.cms.feature.prescription.PrescriptionFixtures.vitals;
import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfRendererTest {

    private final PrescriptionPdfRenderer renderer = new PrescriptionPdfRenderer(
            new ClinicProperties("Sunrise Clinic", "12 Sukhumvit Rd, Bangkok", "02-123-4567",
                    ZoneId.of("Asia/Bangkok")));

    @Test
    void rendersTheConsultationAndPrescription() throws IOException {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        visit.getVitalSigns().add(vitals(visit));
        Prescription prescription = prescription(visit,
                medicine("MED-005", "Loratadine 10mg"), medicine("MED-010", "Dexamethasone 0.5mg"));

        byte[] pdf = renderer.render(prescription, "Malee Siriwan");

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String text = textOf(pdf);
        assertThat(text).contains(
                "Sunrise Clinic", "12 Sukhumvit Rd, Bangkok",
                "PRESCRIPTION", "Rx No. 66000003", "07 Oct 2026",
                "Nattaya Poonpan", "MRN-000002", "34 y / Female", "B+",
                "Dr. Malee Siriwan",
                "Penicillin",
                "118/76 mmHg", "72 bpm", "36.7", "55 kg", "160 cm", "21.5",
                "Itchy rash on both forearms",
                "Allergic contact dermatitis", "L23.9",
                "Loratadine 10mg", "Dexamethasone 0.5mg", "Once daily", "7 tablet", "Take at night",
                "Avoid the irritant",
                "Signature & stamp");
    }

    @Test
    void handlesMissingVitalsAllergiesAndDoctorName() throws IOException {
        Visit visit = visit(VisitStatus.IN_PROGRESS);
        visit.getPatient().setAllergies(null);
        visit.setDiagnosisCode(null);
        Prescription prescription = prescription(visit, medicine("MED-001", "Paracetamol 500mg"));
        prescription.setNotes(null);

        String text = textOf(renderer.render(prescription, null));

        assertThat(text).contains("Not recorded", "None recorded", "Paracetamol 500mg");
        assertThat(text).doesNotContain("Dr. ", "ICD-10", "Advice");
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
