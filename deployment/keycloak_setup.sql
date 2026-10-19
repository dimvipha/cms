-- ============================================================
--  KEYCLOAK CMS REALM SETUP
--  - Creates all realm roles in the cms realm
--  - Moves doctor users from master → cms realm
--  - Assigns DOCTOR role + permissions to each doctor
--
--  Run with:
--    docker exec -i cms-db psql -U cms_user -d keycloak_cms_db -f /dev/stdin < deployment/keycloak_setup.sql
-- ============================================================

\c keycloak_cms_db

-- ─── Variables ────────────────────────────────────────────────────────────────
-- cms realm id: 5c1689a9-8e98-42b6-852f-fe7ffdb2412b

-- ─── 1. Create realm roles in cms realm ──────────────────────────────────────

INSERT INTO keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id)
VALUES
  -- ── Base roles ──────────────────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'System administrator with full access', 'ADMIN',          '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Doctor user',                          'DOCTOR',         '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Nurse user',                           'NURSE',          '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Receptionist user',                    'RECEPTIONIST',   '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Pharmacist user',                      'PHARMACIST',     '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Lab technician user',                  'LAB_TECHNICIAN', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Patient user',                         'PATIENT',        '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Department permissions ───────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read departments',   'department:read',   '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create departments', 'department:write',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Update departments', 'department:update', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Delete departments', 'department:delete', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Doctor permissions ───────────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read doctors',   'doctor:read',   '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create doctors', 'doctor:write',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Update doctors', 'doctor:update', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Delete doctors', 'doctor:delete', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Patient permissions ──────────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read own patient record',    'patient:read',       '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read all patient records',   'patient:read-all',   '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create patient records',     'patient:write',      '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create all patient records', 'patient:write-all',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Update own patient record',  'patient:update',     '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Update all patient records', 'patient:update-all', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Appointment permissions ──────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read appointments',   'appointment:read',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create appointments', 'appointment:write', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Visit / consultation permissions ─────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read visits',                    'visit:read',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Start, update and close visits', 'visit:write', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Record vital signs',             'vitals:write', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Prescription permissions ─────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read and print prescriptions', 'prescription:read',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Write prescriptions',          'prescription:write', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Medicine permissions ─────────────────────────────────────────────────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read medicine catalogue', 'medicine:read', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),

  -- ── Billing permissions (receptionist, pharmacist, admin — not doctors) ─────
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Read and print invoices',          'invoice:read',   '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Create, edit, issue, void invoices', 'invoice:write',  '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'),
  (gen_random_uuid(), '5c1689a9-8e98-42b6-852f-fe7ffdb2412b', false, 'Record payments',                  'payment:record', '5c1689a9-8e98-42b6-852f-fe7ffdb2412b')

ON CONFLICT DO NOTHING;


-- ─── 2. Move doctor users from master realm → cms realm ───────────────────────

UPDATE user_entity
SET realm_id = '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'
WHERE id IN (
  'aaaa0001-0000-0000-0000-000000000001',
  'aaaa0002-0000-0000-0000-000000000002',
  'aaaa0003-0000-0000-0000-000000000003',
  'aaaa0004-0000-0000-0000-000000000004',
  'aaaa0005-0000-0000-0000-000000000005'
);


-- ─── 3. Assign roles to users ─────────────────────────────────────────────────
-- Each doctor gets: DOCTOR + doctor:read/update + patient:read-all/update-all
--                   + appointment:read/write + department:read
--                   + visit:read/write + vitals:write + prescription:read/write + medicine:read

INSERT INTO user_role_mapping (role_id, user_id)
SELECT r.id, u.id
FROM keycloak_role r, user_entity u
WHERE r.realm_id = '5c1689a9-8e98-42b6-852f-fe7ffdb2412b'
  AND r.client_role = false
  AND r.name IN (
    'DOCTOR',
    'doctor:read',
    'doctor:update',
    'patient:read-all',
    'patient:update-all',
    'appointment:read',
    'appointment:write',
    'department:read',
    'visit:read',
    'visit:write',
    'vitals:write',
    'prescription:read',
    'prescription:write',
    'medicine:read'
  )
  AND u.id IN (
    'aaaa0001-0000-0000-0000-000000000001',
    'aaaa0002-0000-0000-0000-000000000002',
    'aaaa0003-0000-0000-0000-000000000003',
    'aaaa0004-0000-0000-0000-000000000004',
    'aaaa0005-0000-0000-0000-000000000005'
  )
ON CONFLICT DO NOTHING;


-- ─── 4. Verify ────────────────────────────────────────────────────────────────

SELECT u.username, string_agg(r.name, ', ' ORDER BY r.name) AS roles
FROM user_entity u
JOIN user_role_mapping m ON m.user_id = u.id
JOIN keycloak_role r     ON r.id = m.role_id
WHERE u.id IN (
  'aaaa0001-0000-0000-0000-000000000001',
  'aaaa0002-0000-0000-0000-000000000002',
  'aaaa0003-0000-0000-0000-000000000003',
  'aaaa0004-0000-0000-0000-000000000004',
  'aaaa0005-0000-0000-0000-000000000005'
)
GROUP BY u.username
ORDER BY u.username;
