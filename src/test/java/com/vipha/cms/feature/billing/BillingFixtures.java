package com.vipha.cms.feature.billing;

import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.laboratory.LabOrder;
import com.vipha.cms.feature.laboratory.LabOrderItem;
import com.vipha.cms.feature.laboratory.LabOrderStatus;
import com.vipha.cms.feature.laboratory.LabTest;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.feature.pharmacy.Medicine;
import com.vipha.cms.feature.prescription.Prescription;
import com.vipha.cms.feature.prescription.PrescriptionItem;
import com.vipha.cms.feature.visit.Visit;
import com.vipha.cms.feature.visit.VisitStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

final class BillingFixtures {

    private BillingFixtures() {}

    static Patient patient() {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setMrn("MRN-000006");
        patient.setFirstName("Pimchanok");
        patient.setLastName("Rattana");
        patient.setDob(LocalDate.of(1998, 9, 20));
        patient.setGender("FEMALE");
        patient.setPhone("0812345606");
        return patient;
    }

    static Visit visit(VisitStatus status) {
        Doctor doctor = new Doctor();
        doctor.setUserId("doctor-1");
        doctor.setActive(true);

        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient());
        visit.setDoctor(doctor);
        visit.setVisitDate(Instant.parse("2026-10-07T02:30:00Z"));
        visit.setStatus(status);
        return visit;
    }

    static Prescription prescription(Visit visit, Object... medicineAndQuantity) {
        Prescription prescription = new Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setVisit(visit);
        for (int i = 0; i < medicineAndQuantity.length; i += 2) {
            PrescriptionItem item = new PrescriptionItem();
            item.setId(UUID.randomUUID());
            item.setPrescription(prescription);
            item.setMedicine((Medicine) medicineAndQuantity[i]);
            item.setQuantity((Integer) medicineAndQuantity[i + 1]);
            prescription.getItems().add(item);
        }
        return prescription;
    }

    static Medicine medicine(String name, String price) {
        Medicine medicine = new Medicine();
        medicine.setId(UUID.randomUUID());
        medicine.setName(name);
        medicine.setUnit("tablet");
        medicine.setPrice(new BigDecimal(price));
        medicine.setActive(true);
        return medicine;
    }

    static LabOrder labOrder(Visit visit, LabOrderStatus status, LabOrderItem... items) {
        LabOrder order = new LabOrder();
        order.setId(UUID.randomUUID());
        order.setVisit(visit);
        order.setStatus(status);
        for (LabOrderItem item : items) {
            item.setLabOrder(order);
            order.getItems().add(item);
        }
        return order;
    }

    static LabOrderItem labItem(String testName, String price, LabOrderStatus status) {
        LabTest test = new LabTest();
        test.setId(UUID.randomUUID());
        test.setName(testName);
        test.setPrice(new BigDecimal(price));

        LabOrderItem item = new LabOrderItem();
        item.setId(UUID.randomUUID());
        item.setLabTest(test);
        item.setStatus(status);
        return item;
    }

    /** An invoice with the given lines ({description, quantity, unitPrice} triples), totals already worked out. */
    static Invoice invoice(InvoiceStatus status, Object... lines) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2026-00007");
        invoice.setPatient(patient());
        invoice.setIssuedAt(Instant.parse("2026-10-07T04:00:00Z"));
        invoice.setStatus(status);
        for (int i = 0; i < lines.length; i += 3) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setItemType(ItemType.OTHER);
            item.setDescription((String) lines[i]);
            item.setQuantity((Integer) lines[i + 1]);
            item.setUnitPrice(new BigDecimal((String) lines[i + 2]));
            item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            invoice.getItems().add(item);
        }
        invoice.recalculateTotals();
        return invoice;
    }

    static Payment payment(Invoice invoice, String amount, PaymentMethod method, String receivedBy) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoice(invoice);
        payment.setAmount(new BigDecimal(amount));
        payment.setMethod(method);
        payment.setPaidAt(Instant.parse("2026-10-07T04:10:00Z"));
        payment.setReceivedBy(receivedBy);
        invoice.getPayments().add(payment);
        invoice.refreshPaymentStatus();
        return payment;
    }
}
