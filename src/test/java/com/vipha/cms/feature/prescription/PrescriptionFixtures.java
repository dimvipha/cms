package com.vipha.cms.feature.prescription;

import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitStatus;
import com.vipha.cms.feature.visit.VitalSign;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

final class PrescriptionFixtures {

    static final String DOCTOR_ID = "doctor-1";

    private PrescriptionFixtures() {}

    static Patient patient() {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setMrn("MRN-000002");
        patient.setFirstName("Nattaya");
        patient.setLastName("Poonpan");
        patient.setDob(LocalDate.of(1992, 7, 25));
        patient.setGender("FEMALE");
        patient.setBloodGroup("B+");
        patient.setAllergies("Penicillin");
        return patient;
    }

    static Doctor doctor() {
        Doctor doctor = new Doctor();
        doctor.setUserId(DOCTOR_ID);
        doctor.setActive(true);
        return doctor;
    }

    static Visit visit(VisitStatus status) {
        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient());
        visit.setDoctor(doctor());
        visit.setVisitDate(Instant.parse("2026-10-07T02:30:00Z"));
        visit.setChiefComplaint("Itchy rash on both forearms");
        visit.setDiagnosisCode("L23.9");
        visit.setDiagnosis("Allergic contact dermatitis");
        visit.setStatus(status);
        return visit;
    }

    static VitalSign vitals(Visit visit) {
        VitalSign vitals = new VitalSign();
        vitals.setVisit(visit);
        vitals.setRecordedAt(Instant.parse("2026-10-07T02:40:00Z"));
        vitals.setSystolicBp(118);
        vitals.setDiastolicBp(76);
        vitals.setPulseRate(72);
        vitals.setTemperatureCelsius(new BigDecimal("36.7"));
        vitals.setWeightKg(new BigDecimal("55.00"));
        vitals.setHeightCm(new BigDecimal("160.00"));
        vitals.setOxygenSaturation(99);
        return vitals;
    }

    static Medicine medicine(String code, String name) {
        Medicine medicine = new Medicine();
        medicine.setId(UUID.randomUUID());
        medicine.setCode(code);
        medicine.setName(name);
        medicine.setUnit("tablet");
        medicine.setActive(true);
        return medicine;
    }

    static Prescription prescription(Visit visit, Medicine... medicines) {
        Prescription prescription = new Prescription();
        prescription.setId(UUID.fromString("66000003-0000-0000-0000-000000000003"));
        prescription.setVisit(visit);
        prescription.setPatient(visit.getPatient());
        prescription.setDoctor(visit.getDoctor());
        prescription.setPrescribedAt(Instant.parse("2026-10-07T03:00:00Z"));
        prescription.setNotes("Avoid the irritant. Return if the rash spreads.");
        for (Medicine medicine : medicines) {
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setDosage("10mg");
            item.setFrequency("Once daily");
            item.setDuration("7 days");
            item.setQuantity(7);
            item.setInstructions("Take at night");
            prescription.getItems().add(item);
        }
        return prescription;
    }
}
