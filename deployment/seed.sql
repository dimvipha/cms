-- ============================================================
--  CMS SEED DATA
--  Run with:
--    docker exec -i cms-db psql -U cms_user -f /dev/stdin < deployment/seed.sql
-- ============================================================

-- ─── KEYCLOAK: create doctor users in the master realm ───────────────────────

\c keycloak_cms_db

INSERT INTO user_entity (id, email, email_constraint, email_verified, enabled, first_name, last_name, realm_id, username, created_timestamp, not_before)
VALUES
  ('aaaa0001-0000-0000-0000-000000000001', 'dr.somchai@cms.local',  'dr.somchai@cms.local',  true, true, 'Somchai',  'Raksa',    'c093187d-27cf-4ff4-9567-8d4a9f6ba873', 'dr.somchai',  extract(epoch from now())::bigint * 1000, 0),
  ('aaaa0002-0000-0000-0000-000000000002', 'dr.nipa@cms.local',     'dr.nipa@cms.local',     true, true, 'Nipa',     'Chantra',  'c093187d-27cf-4ff4-9567-8d4a9f6ba873', 'dr.nipa',     extract(epoch from now())::bigint * 1000, 0),
  ('aaaa0003-0000-0000-0000-000000000003', 'dr.wirat@cms.local',    'dr.wirat@cms.local',    true, true, 'Wirat',    'Phutthon', 'c093187d-27cf-4ff4-9567-8d4a9f6ba873', 'dr.wirat',    extract(epoch from now())::bigint * 1000, 0),
  ('aaaa0004-0000-0000-0000-000000000004', 'dr.malee@cms.local',    'dr.malee@cms.local',    true, true, 'Malee',    'Siriwan',  'c093187d-27cf-4ff4-9567-8d4a9f6ba873', 'dr.malee',    extract(epoch from now())::bigint * 1000, 0),
  ('aaaa0005-0000-0000-0000-000000000005', 'dr.prasong@cms.local',  'dr.prasong@cms.local',  true, true, 'Prasong',  'Klinkaew', 'c093187d-27cf-4ff4-9567-8d4a9f6ba873', 'dr.prasong',  extract(epoch from now())::bigint * 1000, 0)
ON CONFLICT DO NOTHING;

-- ─── CMS DATA ────────────────────────────────────────────────────────────────

\c cms_db

-- ── Departments ──────────────────────────────────────────────────────────────

INSERT INTO department (name, description) VALUES
  ('General Medicine',  'Primary care and general health services'),
  ('Cardiology',        'Heart and cardiovascular diseases'),
  ('Pediatrics',        'Medical care for infants, children, and adolescents'),
  ('Orthopedics',       'Bone, joint, and musculoskeletal conditions'),
  ('Dermatology',       'Skin, hair, and nail conditions')
ON CONFLICT DO NOTHING;

-- ── User profiles (mirrors Keycloak user IDs) ────────────────────────────────

INSERT INTO user_profile (id, gender) VALUES
  ('aaaa0001-0000-0000-0000-000000000001', 'MALE'),
  ('aaaa0002-0000-0000-0000-000000000002', 'FEMALE'),
  ('aaaa0003-0000-0000-0000-000000000003', 'MALE'),
  ('aaaa0004-0000-0000-0000-000000000004', 'FEMALE'),
  ('aaaa0005-0000-0000-0000-000000000005', 'MALE')
ON CONFLICT DO NOTHING;

-- ── Doctors ──────────────────────────────────────────────────────────────────

INSERT INTO doctor (user_id, active, department_id) VALUES
  ('aaaa0001-0000-0000-0000-000000000001', true, (SELECT id FROM department WHERE name = 'General Medicine')),
  ('aaaa0002-0000-0000-0000-000000000002', true, (SELECT id FROM department WHERE name = 'Cardiology')),
  ('aaaa0003-0000-0000-0000-000000000003', true, (SELECT id FROM department WHERE name = 'Pediatrics')),
  ('aaaa0004-0000-0000-0000-000000000004', true, (SELECT id FROM department WHERE name = 'Dermatology')),
  ('aaaa0005-0000-0000-0000-000000000005', true, (SELECT id FROM department WHERE name = 'Orthopedics'))
ON CONFLICT DO NOTHING;

-- ── Doctor specialties ───────────────────────────────────────────────────────

INSERT INTO doctor_specialty (doctor_user_id, specialty) VALUES
  ('aaaa0001-0000-0000-0000-000000000001', 'GENERAL_PRACTITIONER'),
  ('aaaa0002-0000-0000-0000-000000000002', 'CARDIOLOGIST'),
  ('aaaa0003-0000-0000-0000-000000000003', 'PEDIATRICIAN'),
  ('aaaa0004-0000-0000-0000-000000000004', 'DERMATOLOGIST'),
  ('aaaa0005-0000-0000-0000-000000000005', 'ORTHOPEDIST')
ON CONFLICT DO NOTHING;

-- ── Doctor schedules ─────────────────────────────────────────────────────────

INSERT INTO doctor_schedule (doctor_user_id, day_of_week, start_time, end_time) VALUES
  ('aaaa0001-0000-0000-0000-000000000001', 'MONDAY',    '08:00', '17:00'),
  ('aaaa0001-0000-0000-0000-000000000001', 'WEDNESDAY', '08:00', '17:00'),
  ('aaaa0001-0000-0000-0000-000000000001', 'FRIDAY',    '08:00', '12:00'),
  ('aaaa0002-0000-0000-0000-000000000002', 'TUESDAY',   '08:00', '17:00'),
  ('aaaa0002-0000-0000-0000-000000000002', 'THURSDAY',  '08:00', '17:00'),
  ('aaaa0003-0000-0000-0000-000000000003', 'MONDAY',    '08:00', '17:00'),
  ('aaaa0003-0000-0000-0000-000000000003', 'TUESDAY',   '08:00', '17:00'),
  ('aaaa0003-0000-0000-0000-000000000003', 'WEDNESDAY', '08:00', '17:00'),
  ('aaaa0004-0000-0000-0000-000000000004', 'THURSDAY',  '09:00', '17:00'),
  ('aaaa0004-0000-0000-0000-000000000004', 'FRIDAY',    '09:00', '17:00'),
  ('aaaa0005-0000-0000-0000-000000000005', 'MONDAY',    '08:00', '16:00'),
  ('aaaa0005-0000-0000-0000-000000000005', 'WEDNESDAY', '08:00', '16:00'),
  ('aaaa0005-0000-0000-0000-000000000005', 'FRIDAY',    '08:00', '12:00');

-- ── Patients ─────────────────────────────────────────────────────────────────

INSERT INTO patient (id, created_at, active, first_name, last_name, dob, gender, phone, email, address, blood_group, allergies, emergency_contact_name, emergency_contact_phone, mrn) VALUES
  ('b0000001-0000-0000-0000-000000000001', now(), true, 'Somsak',    'Jaidee',    '1985-03-12', 'MALE',   '0812345601', 'somsak@gmail.com',    '12 Sukhumvit Rd, Bangkok',        'A+',  NULL,       'Malee Jaidee',      '0812345699', 'MRN-000001'),
  ('b0000002-0000-0000-0000-000000000002', now(), true, 'Nattaya',   'Poonpan',   '1992-07-25', 'FEMALE', '0812345602', 'nattaya@gmail.com',   '45 Rama IV Rd, Bangkok',          'B+',  'Penicillin','Somchai Poonpan',   '0812345698', 'MRN-000002'),
  ('b0000003-0000-0000-0000-000000000003', now(), true, 'Chaichana', 'Srirak',    '1978-11-30', 'MALE',   '0812345603', 'chaichana@gmail.com', '88 Phahon Yothin Rd, Bangkok',    'O+',  NULL,       'Suda Srirak',       '0812345697', 'MRN-000003'),
  ('b0000004-0000-0000-0000-000000000004', now(), true, 'Anchana',   'Boonsong',  '2001-01-15', 'FEMALE', '0812345604', 'anchana@gmail.com',   '3 Lat Phrao Rd, Bangkok',         'AB-', 'Aspirin',  'Wichai Boonsong',   '0812345696', 'MRN-000004'),
  ('b0000005-0000-0000-0000-000000000005', now(), true, 'Thanakorn', 'Wongsawat', '1965-06-08', 'MALE',   '0812345605', 'thanakorn@gmail.com', '77 Charoen Nakhon Rd, Bangkok',   'B-',  NULL,       'Siriporn Wongsawat','0812345695', 'MRN-000005'),
  ('b0000006-0000-0000-0000-000000000006', now(), true, 'Pimchanok', 'Rattana',   '1998-09-20', 'FEMALE', '0812345606', 'pimchanok@gmail.com', '21 Ngam Wong Wan Rd, Nonthaburi', 'A-',  'Sulfa',    'Krit Rattana',      '0812345694', 'MRN-000006'),
  ('b0000007-0000-0000-0000-000000000007', now(), true, 'Kritsada',  'Thongdee',  '1990-04-02', 'MALE',   '0812345607', 'kritsada@gmail.com',  '56 Bang Na Rd, Bangkok',          'O-',  NULL,       'Usa Thongdee',      '0812345693', 'MRN-000007'),
  ('b0000008-0000-0000-0000-000000000008', now(), true, 'Supattra',  'Limsakul',  '2010-12-05', 'FEMALE', '0812345608', 'supattra@gmail.com',  '9 Ratchada Rd, Bangkok',          'A+',  NULL,       'Panya Limsakul',    '0812345692', 'MRN-000008')
ON CONFLICT DO NOTHING;

-- ── Appointments ─────────────────────────────────────────────────────────────

INSERT INTO appointment (id, created_at, appointment_date, appointment_time, reason, status, department_id, doctor_user_id, patient_id) VALUES
  ('c0000001-0000-0000-0000-000000000001', now(), '2026-09-29', '09:00', 'Annual health checkup',   'SCHEDULED', (SELECT id FROM department WHERE name='General Medicine'), 'aaaa0001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001'),
  ('c0000002-0000-0000-0000-000000000002', now(), '2026-09-29', '10:00', 'Chest pain follow-up',    'CONFIRMED', (SELECT id FROM department WHERE name='Cardiology'),       'aaaa0002-0000-0000-0000-000000000002', 'b0000005-0000-0000-0000-000000000005'),
  ('c0000003-0000-0000-0000-000000000003', now(), '2026-09-30', '08:30', 'Fever and cough',         'SCHEDULED', (SELECT id FROM department WHERE name='Pediatrics'),       'aaaa0003-0000-0000-0000-000000000003', 'b0000008-0000-0000-0000-000000000008'),
  ('c0000004-0000-0000-0000-000000000004', now(), '2026-09-30', '14:00', 'Knee pain consultation',  'SCHEDULED', (SELECT id FROM department WHERE name='Orthopedics'),      'aaaa0005-0000-0000-0000-000000000005', 'b0000003-0000-0000-0000-000000000003'),
  ('c0000005-0000-0000-0000-000000000005', now(), '2026-10-01', '11:00', 'Skin rash evaluation',    'SCHEDULED', (SELECT id FROM department WHERE name='Dermatology'),      'aaaa0004-0000-0000-0000-000000000004', 'b0000002-0000-0000-0000-000000000002'),
  ('c0000006-0000-0000-0000-000000000006', now(), '2026-09-25', '09:00', 'General health check',    'COMPLETED', (SELECT id FROM department WHERE name='General Medicine'), 'aaaa0001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004'),
  ('c0000007-0000-0000-0000-000000000007', now(), '2026-09-26', '10:30', 'Hypertension follow-up',  'COMPLETED', (SELECT id FROM department WHERE name='Cardiology'),       'aaaa0002-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000007'),
  ('c0000008-0000-0000-0000-000000000008', now(), '2026-09-27', '13:00', 'Back pain',               'COMPLETED', (SELECT id FROM department WHERE name='Orthopedics'),      'aaaa0005-0000-0000-0000-000000000005', 'b0000006-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

-- ── Medicines ────────────────────────────────────────────────────────────────

INSERT INTO medicine (id, created_at, active, code, name, form, unit, price, reorder_level) VALUES
  ('d0000001-0000-0000-0000-000000000001', now(), true, 'MED-001', 'Paracetamol 500mg',   'TABLET',  'tablet',  5.00,  100),
  ('d0000002-0000-0000-0000-000000000002', now(), true, 'MED-002', 'Amoxicillin 500mg',   'CAPSULE', 'capsule', 12.00, 50),
  ('d0000003-0000-0000-0000-000000000003', now(), true, 'MED-003', 'Ibuprofen 400mg',     'TABLET',  'tablet',  8.00,  80),
  ('d0000004-0000-0000-0000-000000000004', now(), true, 'MED-004', 'Omeprazole 20mg',     'CAPSULE', 'capsule', 15.00, 60),
  ('d0000005-0000-0000-0000-000000000005', now(), true, 'MED-005', 'Loratadine 10mg',     'TABLET',  'tablet',  10.00, 70),
  ('d0000006-0000-0000-0000-000000000006', now(), true, 'MED-006', 'Atenolol 50mg',       'TABLET',  'tablet',  18.00, 40),
  ('d0000007-0000-0000-0000-000000000007', now(), true, 'MED-007', 'Metformin 500mg',     'TABLET',  'tablet',  9.00,  90),
  ('d0000008-0000-0000-0000-000000000008', now(), true, 'MED-008', 'Amlodipine 5mg',      'TABLET',  'tablet',  22.00, 30),
  ('d0000009-0000-0000-0000-000000000009', now(), true, 'MED-009', 'Cetirizine 10mg',     'TABLET',  'tablet',  7.00,  75),
  ('d0000010-0000-0000-0000-000000000010', now(), true, 'MED-010', 'Dexamethasone 0.5mg', 'TABLET',  'tablet',  6.00,  50)
ON CONFLICT DO NOTHING;

-- ── Medicine batches ──────────────────────────────────────────────────────────

INSERT INTO medicine_batch (id, created_at, batch_number, expiry_date, received_at, stock_quantity, medicine_id) VALUES
  (gen_random_uuid(), now(), 'LOT-P001',  '2027-12-31', '2026-09-01', 500, 'd0000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 'LOT-A001',  '2027-06-30', '2026-09-01', 200, 'd0000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), 'LOT-I001',  '2027-09-30', '2026-09-01', 400, 'd0000003-0000-0000-0000-000000000003'),
  (gen_random_uuid(), now(), 'LOT-O001',  '2028-03-31', '2026-09-01', 300, 'd0000004-0000-0000-0000-000000000004'),
  (gen_random_uuid(), now(), 'LOT-L001',  '2027-11-30', '2026-09-01', 350, 'd0000005-0000-0000-0000-000000000005'),
  (gen_random_uuid(), now(), 'LOT-AT001', '2027-08-31', '2026-09-01', 150, 'd0000006-0000-0000-0000-000000000006'),
  (gen_random_uuid(), now(), 'LOT-M001',  '2027-10-31', '2026-09-01', 450, 'd0000007-0000-0000-0000-000000000007'),
  (gen_random_uuid(), now(), 'LOT-AM001', '2028-01-31', '2026-09-01', 100, 'd0000008-0000-0000-0000-000000000008')
ON CONFLICT DO NOTHING;

-- ── Lab tests ────────────────────────────────────────────────────────────────

INSERT INTO lab_test (id, created_at, active, code, name, description, price) VALUES
  ('e0000001-0000-0000-0000-000000000001', now(), true, 'LAB-001', 'Complete Blood Count (CBC)',   'Full blood panel including RBC, WBC, platelets', 250.00),
  ('e0000002-0000-0000-0000-000000000002', now(), true, 'LAB-002', 'Fasting Blood Glucose',        'Blood sugar level after fasting',                 120.00),
  ('e0000003-0000-0000-0000-000000000003', now(), true, 'LAB-003', 'Lipid Panel',                  'Cholesterol, LDL, HDL, triglycerides',            350.00),
  ('e0000004-0000-0000-0000-000000000004', now(), true, 'LAB-004', 'Liver Function Test (LFT)',    'ALT, AST, ALP, bilirubin',                        400.00),
  ('e0000005-0000-0000-0000-000000000005', now(), true, 'LAB-005', 'Renal Function Test (RFT)',    'Creatinine, BUN, uric acid',                      380.00),
  ('e0000006-0000-0000-0000-000000000006', now(), true, 'LAB-006', 'Thyroid Stimulating Hormone',  'TSH level',                                       450.00),
  ('e0000007-0000-0000-0000-000000000007', now(), true, 'LAB-007', 'Urinalysis',                   'Urine color, pH, protein, glucose, cells',        150.00),
  ('e0000008-0000-0000-0000-000000000008', now(), true, 'LAB-008', 'Chest X-Ray',                  'Radiograph of the chest',                         500.00),
  ('e0000009-0000-0000-0000-000000000009', now(), true, 'LAB-009', 'ECG / EKG',                    '12-lead electrocardiogram',                       300.00),
  ('e0000010-0000-0000-0000-000000000010', now(), true, 'LAB-010', 'HbA1c',                        'Average blood sugar over 3 months',               280.00)
ON CONFLICT DO NOTHING;

-- ── Completed visits ──────────────────────────────────────────────────────────

INSERT INTO visit (id, created_at, visit_date, status, chief_complaint, diagnosis, notes, appointment_id, doctor_user_id, patient_id) VALUES
  ('f0000001-0000-0000-0000-000000000001', now(), '2026-09-25 09:00:00+07', 'COMPLETED',
   'Routine checkup', 'Healthy, no acute issues', 'Advised lifestyle changes and follow-up in 6 months',
   'c0000006-0000-0000-0000-000000000006', 'aaaa0001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004'),

  ('f0000002-0000-0000-0000-000000000002', now(), '2026-09-26 10:30:00+07', 'COMPLETED',
   'Hypertension follow-up', 'Stage 1 hypertension, controlled', 'Continue Amlodipine 5mg, recheck in 1 month',
   'c0000007-0000-0000-0000-000000000007', 'aaaa0002-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000007'),

  ('f0000003-0000-0000-0000-000000000003', now(), '2026-09-27 13:00:00+07', 'COMPLETED',
   'Lumbar back pain', 'Lumbar muscle strain', 'Rest, Ibuprofen 400mg TID for 5 days, physiotherapy referral',
   'c0000008-0000-0000-0000-000000000008', 'aaaa0005-0000-0000-0000-000000000005', 'b0000006-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

-- ── Vital signs ───────────────────────────────────────────────────────────────

INSERT INTO vital_sign (id, created_at, recorded_at, systolic_bp, diastolic_bp, pulse_rate, height_cm, weight_kg, temperature_celsius, oxygen_saturation, respiratory_rate, visit_id) VALUES
  (gen_random_uuid(), now(), '2026-09-25 09:10:00+07',  120, 80, 72, 165.0, 61.2, 36.8, 99, 16, 'f0000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), '2026-09-26 10:40:00+07',  145, 92, 88, 170.0, 80.5, 37.0, 98, 18, 'f0000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), '2026-09-27 13:10:00+07',  118, 78, 76, 168.0, 68.0, 36.6, 99, 17, 'f0000003-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- ── Prescriptions ─────────────────────────────────────────────────────────────

INSERT INTO prescription (id, created_at, prescribed_at, visit_id, doctor_user_id, patient_id) VALUES
  ('66000001-0000-0000-0000-000000000001', now(), '2026-09-26 11:00:00+07', 'f0000002-0000-0000-0000-000000000002', 'aaaa0002-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000007'),
  ('66000002-0000-0000-0000-000000000002', now(), '2026-09-27 14:00:00+07', 'f0000003-0000-0000-0000-000000000003', 'aaaa0005-0000-0000-0000-000000000005', 'b0000006-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

INSERT INTO prescription_item (id, created_at, dosage, duration, frequency, instructions, quantity, medicine_id, prescription_id) VALUES
  (gen_random_uuid(), now(), '5mg',  '30 days', 'ONCE_DAILY',  'Take in the morning', 30, 'd0000008-0000-0000-0000-000000000008', '66000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), '400mg', '5 days', 'THREE_TIMES', 'Take with food',       15, 'd0000003-0000-0000-0000-000000000003', '66000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), '500mg', '5 days', 'THREE_TIMES', 'Take after meals',     15, 'd0000001-0000-0000-0000-000000000001', '66000002-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- ── Lab orders ────────────────────────────────────────────────────────────────

INSERT INTO lab_order (id, created_at, ordered_at, status, visit_id, doctor_user_id, patient_id) VALUES
  ('77000001-0000-0000-0000-000000000001', now(), '2026-09-25 09:15:00+07', 'COMPLETED', 'f0000001-0000-0000-0000-000000000001', 'aaaa0001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004'),
  ('77000002-0000-0000-0000-000000000002', now(), '2026-09-26 10:45:00+07', 'COMPLETED', 'f0000002-0000-0000-0000-000000000002', 'aaaa0002-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000007')
ON CONFLICT DO NOTHING;

INSERT INTO lab_order_item (id, created_at, result, result_at, status, lab_test_id, lab_order_id) VALUES
  (gen_random_uuid(), now(), 'WBC: 6.5, RBC: 4.8, Hb: 14.2, Plt: 250', '2026-09-25 14:00:00+07', 'COMPLETED', 'e0000001-0000-0000-0000-000000000001', '77000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 'Glucose: 95 mg/dL (Normal)',               '2026-09-25 14:00:00+07', 'COMPLETED', 'e0000002-0000-0000-0000-000000000002', '77000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 'Total: 210, LDL: 130, HDL: 55, TG: 125',  '2026-09-26 15:00:00+07', 'COMPLETED', 'e0000003-0000-0000-0000-000000000003', '77000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), 'Normal sinus rhythm, HR 88 bpm',           '2026-09-26 11:30:00+07', 'COMPLETED', 'e0000009-0000-0000-0000-000000000009', '77000002-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

-- ── Invoices ──────────────────────────────────────────────────────────────────

INSERT INTO invoice (id, created_at, invoice_number, issued_at, status, subtotal, tax, discount, amount_paid, total, visit_id, patient_id) VALUES
  ('88000001-0000-0000-0000-000000000001', now(), 'INV-2026-0001', '2026-09-25 15:00:00+07', 'PAID',   620.00, 0.00, 0.00, 620.00,  620.00,  'f0000001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004'),
  ('88000002-0000-0000-0000-000000000002', now(), 'INV-2026-0002', '2026-09-26 16:00:00+07', 'PAID',  1322.00, 0.00, 0.00, 1322.00, 1322.00, 'f0000002-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000007'),
  ('88000003-0000-0000-0000-000000000003', now(), 'INV-2026-0003', '2026-09-27 17:00:00+07', 'UNPAID', 822.00, 0.00, 0.00,   0.00,   822.00, 'f0000003-0000-0000-0000-000000000003', 'b0000006-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

INSERT INTO invoice_item (id, created_at, description, item_type, quantity, unit_price, subtotal, invoice_id) VALUES
  (gen_random_uuid(), now(), 'Consultation fee',      'CONSULTATION', 1,   250.00, 250.00, '88000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 'Complete Blood Count',  'LAB_TEST',     1,   250.00, 250.00, '88000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 'Fasting Blood Glucose', 'LAB_TEST',     1,   120.00, 120.00, '88000001-0000-0000-0000-000000000001'),

  (gen_random_uuid(), now(), 'Consultation fee',      'CONSULTATION', 1,   250.00,  250.00, '88000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), 'Lipid Panel',           'LAB_TEST',     1,   350.00,  350.00, '88000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), 'ECG / EKG',             'LAB_TEST',     1,   300.00,  300.00, '88000002-0000-0000-0000-000000000002'),
  (gen_random_uuid(), now(), 'Amlodipine 5mg',        'MEDICINE',    30,    22.00,  660.00, '88000002-0000-0000-0000-000000000002'),

  (gen_random_uuid(), now(), 'Consultation fee',      'CONSULTATION', 1,   250.00,  250.00, '88000003-0000-0000-0000-000000000003'),
  (gen_random_uuid(), now(), 'Ibuprofen 400mg',       'MEDICINE',    15,     8.00,  120.00, '88000003-0000-0000-0000-000000000003'),
  (gen_random_uuid(), now(), 'Paracetamol 500mg',     'MEDICINE',    15,     5.00,   75.00, '88000003-0000-0000-0000-000000000003'),
  (gen_random_uuid(), now(), 'X-Ray Lumbar Spine',    'PROCEDURE',    1,   500.00,  500.00, '88000003-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- ── Payments ─────────────────────────────────────────────────────────────────

INSERT INTO payment (id, created_at, amount, method, paid_at, invoice_id) VALUES
  (gen_random_uuid(), now(), 620.00,  'CASH', '2026-09-25 15:30:00+07', '88000001-0000-0000-0000-000000000001'),
  (gen_random_uuid(), now(), 1322.00, 'CARD', '2026-09-26 16:30:00+07', '88000002-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;
