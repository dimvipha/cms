# Clinic Management System — API

The REST API behind a small clinic: patient records, appointment booking, consultations with vital
signs, prescriptions, pharmacy stock, and billing with invoices, payments and printable PDFs. People
sign in with Keycloak, and what each person may do comes from their role: admin, doctor, nurse,
receptionist, pharmacist, lab technician or patient.

The web app that uses it lives in its own repository,
[clinic-frontend](https://github.com/dimvipha/clinic-frontend).

> Academic project. Not certified for clinical use, and not HIPAA or GDPR compliant as it stands.

---

## Contents

- [What it does](#what-it-does)
- [Tech stack](#tech-stack)
- [Quick start](#quick-start)
- [Signing in](#signing-in)
- [Roles and permissions](#roles-and-permissions)
- [API overview](#api-overview)
- [Business rules](#business-rules)
- [Architecture](#architecture)
- [Data model](#data-model)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment folder](#deployment-folder)
- [Production checklist](#production-checklist)
- [Known gaps](#known-gaps)
- [Development conventions](#development-conventions)
- [Author](#author)

---

## What it does

| Area | What is built |
|---|---|
| Sign-in | Keycloak's page for everyone (authorization code with PKCE), sign-up, Google, staff invites with two-factor setup, password endpoints for API clients in development |
| Staff | Admins invite staff by email; Keycloak holds their accounts and roles |
| Departments and doctors | Departments, doctor profiles with specialties and weekly schedules, activation |
| Patients | Records with an MRN, self-registration, search, a patient's own portal (visits, prescriptions, invoices and their PDFs) |
| Appointments | 30-minute slots from the doctor's schedule, booking, rescheduling, cancelling, check-in, no-shows |
| Visits | From an appointment or as a walk-in; vital signs, ICD-10 diagnosis, notes, open → in progress → completed |
| Care scope | A doctor sees only the patients booked with them or seen by them |
| Prescriptions | One per visit, written by its doctor, printable as an A4 PDF |
| Pharmacy | Medicine catalogue, stock in batches with expiry dates, low-stock list |
| Billing | Invoices drafted from a completed visit or by hand, issued, voided, paid in parts; invoice or receipt PDF |
| Public | The clinic's departments and bookable doctors for the landing page, no login needed |

Lab orders, reports and dispensing are modelled but not built yet; see [Known gaps](#known-gaps).

---

## Tech stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java (Gradle toolchain) | 26 |
| Build | Gradle (Groovy DSL), wrapper included | 9.7.1 |
| Framework | Spring Boot | 4.1.1 |
| Web, security | Spring Framework / Spring Security (OAuth2 resource server) | 7.0 / 7.1 |
| Persistence | Spring Data JPA, Hibernate | 4.1 / 7.4 |
| Database | PostgreSQL (JDBC driver 42.7) | 17 |
| Identity | Keycloak, Keycloak admin client | latest image / 26.0.12 |
| JSON | Jackson (`tools.jackson`) | 3.1 |
| Mapping, boilerplate | MapStruct, Lombok | 1.6.3 / 1.18 |
| PDF | OpenPDF | 3.0.5 |
| API docs | springdoc-openapi (Swagger UI) | 3.1.1 |
| Tests | JUnit Jupiter, Mockito, AssertJ, Spring Security Test | 6.0 / 5.23 / 3.27 |
| Local services | Docker Compose: Postgres, Keycloak, Mailpit | |

Versions without a pin in `build.gradle` come from the Spring Boot BOM; leave them to it.

Things that differ from most Spring Boot 3 tutorials:

- **Jackson 3.** The package root moved from `com.fasterxml.jackson` to `tools.jackson`; only the
  annotations (`@JsonProperty` and friends) stayed where they were.
- **Annotation processors.** Lombok and MapStruct are declared as `annotationProcessor`, which recent
  JDKs need before they run any processor at all. `lombok-mapstruct-binding` makes MapStruct see
  Lombok's getters.
- **Java 26 toolchain.** Gradle needs a JDK 26 installed; it does not download one.

---

## Quick start

You need JDK 26, Docker with Compose, and `jq` and `curl` for the setup scripts.

```bash
# 1. Once: the Docker network the containers share
docker network create clinic-net

# 2. Postgres (1200), Keycloak (1201) and Mailpit (1202)
cp deployment/.env.example deployment/.env    # set the Postgres and Keycloak admin logins
(cd deployment && docker compose up -d)

# 3. When Keycloak answers on http://localhost:1201: realm, roles, clients, token rules
./deployment/keycloak_setup.sh

# 4. The API's settings
cp src/main/resources/.env.example src/main/resources/.env
#    DB_PASSWORD             the POSTGRES_PASSWORD from deployment/.env
#    KEYCLOAK_CLIENT_SECRET  Keycloak admin console → realm clinic → Clients → private-app → Credentials

# 5. Run the API; its first start creates the tables
bash ./gradlew bootRun

# 6. In another terminal: demo data and one login per role
./deployment/seed.sh
```

| Service | Address |
|---|---|
| API | http://localhost:1300/api/v1 |
| Swagger UI | http://localhost:1300/swagger-ui.html |
| Keycloak admin console | http://localhost:1201 (the `KC_ADMIN_*` login in `deployment/.env`) |
| Mailpit, the outgoing mail of development | http://localhost:1202 |
| Postgres | `localhost:1200`, databases `clinic_db` and `keycloak_clinic_db`, user `clinic_user` |

`seed.sh` adds these logins: `admin@cms.local`; five doctors, `dr.somchai@cms.local`,
`dr.nipa@cms.local`, `dr.wirat@cms.local`, `dr.malee@cms.local` and `dr.prasong@cms.local`;
`nurse@cms.local`, `reception@cms.local`, `pharmacist@cms.local` and `labtech@cms.local`. Their
passwords are `SEED_PASSWORD` and `SEED_ADMIN_PASSWORD`; the defaults are at the top of the script.
Patients sign up themselves.

Useful Gradle tasks:

```bash
bash ./gradlew bootRun    # run the API
bash ./gradlew test       # all tests (two of them need the database running)
bash ./gradlew build      # compile, test and package build/libs/cms-0.0.1-SNAPSHOT.jar
```

---

## Signing in

Keycloak (realm `clinic`) holds every account, password and role; this database has no password
column. The API is an OAuth2 resource server: it checks each request's JWT (issuer, signature,
audience) and reads the roles from `realm_access.roles`.

| Keycloak client | Kind | Used for |
|---|---|---|
| `cms-app` | public, authorization code with PKCE (S256) | the web app: everyone signs in on Keycloak's page |
| `private-app` | confidential, with a service account | the API itself: creating users, assigning roles and sending emails through Keycloak's admin API; in development also the password login of `POST /api/v1/auth/login` |

### The web app

The browser goes to Keycloak's page, comes back to the app with a code, and trades it for tokens
at Keycloak; it refreshes and logs out there too. Keycloak's page also does sign-up (with the email
as username), forgotten passwords and Google. After each sign-in the app calls
`POST /api/v1/auth/welcome`: someone new to the clinic becomes a patient (the `PATIENT` role, the
`patient-management` group and a profile), and the app fetches tokens carrying the role. Staff and
existing patients are left as they are.

### Staff invites

An admin calls `POST /api/v1/staff`. The API creates the Keycloak user with the chosen role (and an
inactive doctor profile for a doctor), and Keycloak emails a link, valid for 12 hours, to set a
password, confirm the email address and set up two-factor sign-in with an authenticator app. In
development the email lands in Mailpit. `POST /api/v1/staff/{id}/reset-password` sends a reset
link valid for an hour.

### Google

1. In Google Cloud, create an OAuth client (type *Web application*) whose authorized redirect URI
   is `http://localhost:1201/realms/clinic/broker/google/endpoint`.
2. Put its ID and secret in `deployment/.env` as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
3. Run `deployment/keycloak_setup.sh`, which adds Google as an identity provider.

`GET /api/v1/auth/google` then reports `enabled: true` and the app shows *Continue with Google*. A
Google account whose email matches an existing user is linked once that user proves the account is
theirs, and keeps its roles.

### Sign-in security

Following the OAuth 2.0 Security Best Current Practice (RFC 9700) and OpenID Connect:

- **Exact redirect URIs.** `cms-app` accepts only `<origin>/auth/callback` (sign-in) and
  `<origin>/login` (the links in invite and reset emails), and logout returns only to
  `<origin>/login`. No wildcards.
- **PKCE, state and nonce.** Every sign-in carries a PKCE challenge, a `state` and a `nonce`; the
  app refuses an answer whose state, or an ID token whose nonce, is not its own.
- **Audience.** Access tokens name the API (`aud: clinic-api`, from the `clinic-api-audience` client
  scope), and the API refuses any other token. Run `keycloak_setup.sh` before starting an API with
  this check, or every token is refused.
- **Short tokens, single-use refresh tokens.** Access tokens last 5 minutes; each refresh token works
  once, so a stolen one that is replayed is refused; a session ends after 30 idle minutes or 10
  hours.
- **Two-factor sign-in for staff.** Invited staff set it up with their password;
  `STAFF_TWO_FACTOR=true ./deployment/keycloak_setup.sh` asks existing staff too, at their next
  sign-in.
- **No password grant in production.** `POST /api/v1/auth/register`, `/login`, `/refresh`,
  `/logout`, `/forgot-password` and `/change-password` exist for API clients such as Postman while
  `PASSWORD_ENDPOINTS` is true (the default). The `prod` profile turns them off. An account with a
  setup step pending, such as two-factor, cannot use them until it finishes that on Keycloak's page.
- **Stateless.** No server session and no cookies, so no CSRF token; CORS admits only the web app's
  origins (`FRONTEND_ALLOWED_ORIGINS`).

Getting a token by hand in development, for curl or Swagger:

```bash
curl -s -X POST http://localhost:1300/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "dr.somchai@cms.local", "password": "<SEED_PASSWORD>"}'

curl http://localhost:1300/api/v1/patients -H "Authorization: Bearer <access_token>"
```

---

## Roles and permissions

Two tiers. The API checks fine-grained permissions such as `invoice:write`; each job role is a
Keycloak composite role carrying its permissions. A staff member is given one job role; patients
get theirs from the `patient-management` group. `deployment/keycloak_setup.sh` is the source of this
table and keeps Keycloak in line with it.

| Permission | What it allows | ADMIN | DOCTOR | NURSE | RECEPTIONIST | PHARMACIST | LAB_TECHNICIAN | PATIENT |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| `department:read` | Read departments | ✓ | ✓ | ✓ | ✓ | | | ✓ |
| `department:write` | Create departments | ✓ | | | | | | |
| `department:update` | Update departments | ✓ | | | | | | |
| `department:delete` | Delete departments | ✓ | | | | | | |
| `doctor:read` | Read doctors | ✓ | ✓ | ✓ | ✓ | | | ✓ |
| `doctor:write` | Activate doctors, set their department and specialties | ✓ | | | | | | |
| `doctor:update` | Update own doctor schedule | ✓ | ✓ | | | | | |
| `doctor:delete` | Delete doctors | ✓ | | | | | | |
| `patient:read` | Read own patient record | ✓ | | | | | | ✓ |
| `patient:read-all` | Read all patient records | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| `patient:write` | Create patient records | ✓ | | | | | | ✓ |
| `patient:write-all` | Create all patient records | ✓ | | | ✓ | | | |
| `patient:update` | Update own patient record | ✓ | | | | | | ✓ |
| `patient:update-all` | Update all patient records | ✓ | ✓ | ✓ | ✓ | | | |
| `appointment:read` | Read own appointments, as patient or doctor | ✓ | ✓ | | | | | ✓ |
| `appointment:read-all` | Read all appointments | ✓ | | ✓ | ✓ | | | |
| `appointment:write` | Book and change own appointments | ✓ | | | | | | ✓ |
| `appointment:write-all` | Book, change, check in and mark no-shows for any patient | ✓ | | | ✓ | | | |
| `visit:read` | Read visits | ✓ | ✓ | ✓ | | | ✓ | |
| `visit:write` | Start, update and close visits | ✓ | ✓ | | | | | |
| `vitals:write` | Record vital signs | ✓ | ✓ | ✓ | | | | |
| `prescription:read` | Read and print prescriptions | ✓ | ✓ | ✓ | | ✓ | | |
| `prescription:write` | Write prescriptions | ✓ | ✓ | | | | | |
| `medicine:read` | Read medicine catalogue | ✓ | ✓ | ✓ | | ✓ | | |
| `medicine:write` | Add and edit medicines and stock | ✓ | | | | ✓ | | |
| `invoice:read` | Read and print invoices | ✓ | | | ✓ | ✓ | | |
| `invoice:write` | Create, edit, issue, void invoices | ✓ | | | ✓ | | | |
| `payment:record` | Record payments | ✓ | | | ✓ | ✓ | | |

On top of the permissions, the services check ownership:

- A patient reaches only their own record, appointments, visits, prescriptions and issued invoices.
- A doctor reads every patient's record (`patient:read-all`) only within their **care scope**:
  patients booked with them, or seen by them in a visit. Staff with a clinic-wide job (admin, nurse,
  receptionist, pharmacist, lab technician) are not limited. See `feature/care/CareScope`.
- Only the visit's own doctor (or an admin) changes a visit or writes its prescription.
- Staff management (`/staff`), deleting a patient and listing every user profile need the `ADMIN`
  role itself.

---

## API overview

Everything is under `/api/v1` and needs a bearer token, except the [public](#public) calls and
Swagger (`/swagger-ui.html`, `/v3/api-docs`). The access rules are URL rules in
`security/SecurityConfig.java`.

**Conventions**

- Lists are paged with `pageNumber` (from 0) and `pageSize` (default 10) and come back as
  `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}`. Exceptions:
  `GET /patients` takes `size`, `GET /user-profiles/all` takes `page` and `size`, and
  `GET /appointments/me` returns a plain list.
- PDFs (prescriptions, invoices, receipts) are A4 and come back inline with a file name in
  `Content-Disposition`.
- Errors share one shape:

  ```json
  {
    "code": 400,
    "success": false,
    "message": "Validation failed",
    "timestamp": "2026-10-22T04:49:13.514Z",
    "errorDetail": [{ "field": "phone", "message": "Phone must be 8-15 digits, optionally starting with +" }]
  }
  ```

  400 for a broken rule or invalid input (with `errorDetail` listing the fields), 401 without a
  valid token, 403 without the permission, 404 for an unknown id, 409 for a clash with existing data.

### Public

| Method | Path | |
|---|---|---|
| `GET` | `/public/clinic` | departments and the doctors taking bookings, for the landing page |
| `GET` | `/auth/google` | whether sign-in with Google is set up |

### Accounts

| Method | Path | Needs |
|---|---|---|
| `POST` | `/auth/welcome` | any login; makes someone new a patient |
| `POST` | `/auth/register` · `/login` · `/refresh` · `/logout` · `/forgot-password` | no login; development only |
| `POST` | `/auth/change-password` | a login; development only |
| `GET` · `PATCH` | `/user-profiles` | a login; your own profile |
| `GET` | `/user-profiles/all` | `ADMIN` |
| `POST` | `/staff` (invite) · `/staff/{id}/reset-password` | `ADMIN` |

### Clinic set-up

| Method | Path | Needs |
|---|---|---|
| `GET` | `/departments` · `/departments/{id}` | `department:read` |
| `POST` · `PUT` · `DELETE` | `/departments` · `/departments/{id}` | `department:write` · `:update` · `:delete` |
| `GET` | `/doctors?departmentId=&specialty=&active=` · `/doctors/me` · `/doctors/{id}` · `/doctors/{id}/schedule` | `doctor:read` |
| `GET` | `/doctors/{id}/availability?date=` (free slots) | `doctor:read` |
| `POST` | `/doctors/{id}/activate` · `/deactivate` | `doctor:write` |
| `PUT` | `/doctors/{id}/department` · `/doctors/{id}/specialties` | `doctor:write` |
| `PUT` | `/doctors/{id}/schedule` | `doctor:update` (own) or `doctor:write` |

### Patients and appointments

| Method | Path | Needs |
|---|---|---|
| `GET` | `/patients?search=` | `patient:read-all` |
| `GET` | `/patients/me` · `/patients/{id}` | `patient:read` (own) or `patient:read-all` |
| `POST` | `/patients` | `patient:write` (own) or `patient:write-all` |
| `PUT` | `/patients/{id}` | `patient:update` (own) or `patient:update-all` |
| `DELETE` | `/patients/{id}` (deactivates) | `ADMIN` |
| `GET` | `/patients/me/visits` · `/prescriptions` · `/prescriptions/{id}/pdf` · `/invoices` · `/invoices/{id}/pdf` | `patient:read` (the caller's own) |
| `GET` | `/appointments` · `/appointments/me` · `/appointments/{id}` | `appointment:read` (own) or `appointment:read-all` |
| `POST` · `PATCH` | `/appointments` · `/appointments/{id}` (reschedule) | `appointment:write` (own) or `appointment:write-all` |
| `POST` | `/appointments/{id}/cancel` | `appointment:write` (own) or `appointment:write-all` |
| `POST` | `/appointments/{id}/check-in` · `/no-show` | `appointment:write-all` |

### Consultations

| Method | Path | Needs |
|---|---|---|
| `GET` | `/visits` · `/visits/me` (the doctor's own) · `/visits/{id}` | `visit:read` |
| `POST` | `/visits` (from `appointmentId`, or a walk-in `patientId`) | `visit:write` |
| `PATCH` | `/visits/{id}` (complaint, ICD-10 code, diagnosis, notes) | `visit:write` |
| `POST` | `/visits/{id}/start` · `/complete` · `/cancel` | `visit:write` |
| `POST` | `/visits/{id}/vitals` | `vitals:write` |
| `GET` | `/prescriptions?visitId=&patientId=` · `/prescriptions/{id}` · `/prescriptions/{id}/pdf` | `prescription:read` |
| `POST` · `PUT` · `DELETE` | `/prescriptions` · `/prescriptions/{id}` | `prescription:write` |

### Pharmacy and billing

| Method | Path | Needs |
|---|---|---|
| `GET` | `/medicines?search=` · `/medicines/low-stock` · `/medicines/{id}` · `/medicines/{id}/batches` | `medicine:read` |
| `POST` · `PUT` | `/medicines` · `/medicines/{id}` · `/medicines/{id}/batches` (receive stock) | `medicine:write` |
| `GET` | `/invoices?patientId=&visitId=&status=` · `/invoices/billable-visits` · `/invoices/{id}` · `/invoices/{id}/pdf` | `invoice:read` |
| `POST` | `/invoices/from-visit/{visitId}` · `/invoices` (by hand) | `invoice:write` |
| `PUT` | `/invoices/{id}` (a draft's lines, tax, discount) | `invoice:write` |
| `POST` | `/invoices/{id}/issue` · `/invoices/{id}/void` | `invoice:write` |
| `POST` | `/invoices/{id}/payments` | `payment:record` |

---

## Business rules

**Appointments** (`feature/appointment`)

- Slots are 30 minutes inside the doctor's schedule for that weekday. Booked times (anything but
  cancelled) and times already past are not offered; past dates and inactive doctors have none.
- Booking needs an active patient, an active doctor and a free slot, and a patient cannot hold two
  appointments at the same time. Patients book for themselves; the front desk names the patient.
- Rescheduling only while scheduled or confirmed; no cancelling once in progress, completed or
  marked a no-show; check-in only on the day; a no-show only after the start time.

**Doctors** (`feature/doctor`): schedule entries on one day must not overlap, and saving replaces
the week. A doctor can be activated (offered for booking) once they have a department, at least one
specialty and a schedule.

**Visits** (`feature/visit`)

- Opened from a scheduled, confirmed or checked-in appointment (one visit each, and the appointment
  moves to in progress), or as a walk-in.
- Open → in progress → completed, or cancelled from either; completing needs a diagnosis and
  completes the appointment. Closed visits are read-only.
- Vital signs need at least one measurement, systolic above diastolic, and an open visit; BMI is
  worked out from height and weight.

**Prescriptions** (`feature/prescription`): one per visit, while the visit is open; no medicine twice,
and only active medicines. Editing replaces the items. A cancelled visit's prescription cannot be
printed.

**Stock** (`feature/pharmacy`): stock arrives in batches with a batch number unique per medicine and a
future expiry date. Usable stock is the sum of the unexpired batches; a medicine is low when that
falls below its reorder level. Medicine codes are never reused.

**Invoices and payments** (`feature/billing`)

- Drafted from a completed visit: the consultation fee (`CONSULTATION_FEE`), the prescribed
  medicines at today's prices, and lab items. One live (not void) invoice per visit, enforced by a
  partial unique index.
- Only drafts can be edited; a discount cannot take the total below zero. Issuing makes it unpaid
  (or paid, for a zero total); after that its status follows the payments.
- Payments cannot exceed the balance and are refused on drafts, paid and void invoices. Voiding is
  refused once anything is paid. Both lock the invoice row so two clerks cannot race.
- Numbers look like `INV-2026-00042`; the PDF says *Invoice* while unpaid and *Receipt* after.

**Patients** (`feature/patient`): MRNs look like `P-2026-00042`. A phone number belongs to one active
patient. A patient signing up links the record to their own account, one record each. Deleting a
patient deactivates them.

---

## Architecture

Package by feature: each feature holds its own controller, service, repository, entities, DTOs and
mapper, so a change to billing stays in `feature/billing`.

```
HTTP ─► SecurityConfig (JWT, URL rules) ─► Controller ─► Service ─► Repository ─► PostgreSQL
                                           DTO records    rules,      Spring Data JPA,
                                           @Valid         ownership,  Specifications
                                                          @Transactional
```

- Controllers speak DTOs (Java records); MapStruct maps entities to them. No entity leaves a service.
- Services are an interface plus `*ServiceImpl`; business rules and ownership checks live there and
  throw `BusinessRuleException` (400) or `ResourceNotFoundException` (404).
- `GlobalExceptionHandler` and `JsonSecurityErrorHandler` turn every failure into the error shape
  above.
- JPA auditing fills `createdAt`, `updatedAt`, `createdBy` and `updatedBy` (the caller's Keycloak id).

```
src/main/java/com/vipha/cms/
├── CmsApplication.java
├── common/          BaseEntity, auditing, ClinicProperties, FrontendProperties,
│                    error handling, error DTOs, PDF layout helpers
├── security/        SecurityConfig, JSON 401/403, roles and permissions,
│                    Keycloak admin client, token calls, user service, name cache
└── feature/
    ├── auth/        welcome, Google option, password endpoints (development)
    ├── staff/       staff invites (accounts live in Keycloak)
    ├── userprofile/ profile per Keycloak user
    ├── department/
    ├── doctor/      doctors, specialties, weekly schedules
    ├── directory/   public landing-page data
    ├── patient/     records, MRNs, the patient's own portal
    ├── appointment/ booking and availability
    ├── care/        CareScope: a doctor's own patients
    ├── visit/       consultations and vital signs
    ├── prescription/ prescriptions and their PDF
    ├── pharmacy/    medicines and stock batches
    ├── billing/     invoices, payments, invoice and receipt PDF
    ├── laboratory/  lab entities only (not built yet)
    └── report/      empty (not built yet)

src/main/resources/
├── application.properties        defaults, each overridable from .env or the environment
├── application-prod.properties   schema validated only, SQL logging off, password endpoints off
├── schema.sql                    sequences and a partial index (runs on every start)
└── .env.example                  the API's settings to copy into .env
```

---

## Data model

| Group | Tables |
|---|---|
| People | `patient`, `user_profile` (one per Keycloak user), `doctor`, `doctor_specialty`, `doctor_schedule`, `department` |
| Scheduling and care | `appointment`, `visit`, `vital_sign` |
| Medicines | `prescription`, `prescription_item`, `medicine`, `medicine_batch` |
| Billing | `invoice`, `invoice_item`, `payment` |
| Lab (not built yet) | `lab_test`, `lab_order`, `lab_order_item` |

- Identity lives in Keycloak. `doctor.user_id`, `user_profile.id` and `patient.keycloak_id` hold the
  Keycloak user id (the JWT `sub`).
- A patient has many visits; a visit has at most one prescription and one live invoice. An
  appointment leads to at most one visit; walk-ins have none.
- `invoice_item` keeps its own description and unit price, so an old invoice does not change when a
  price does. `ref_id` points at what was billed, by `item_type`, without a foreign key.
- Money is `BigDecimal` / `NUMERIC`; enums are stored as strings; ids are UUIDs (departments and
  schedule rows use numbers).

**Schema.** Hibernate creates and updates the tables in development (`ddl-auto=update`), and
`schema.sql` then adds the MRN and invoice-number sequences and the one-live-invoice-per-visit index.
The `prod` profile only validates the schema; there are no migrations yet.

**Demo data.** `deployment/seed.sql` (loaded by `seed.sh`) adds 5 departments, 5 doctors with
schedules, 8 patients, appointments, medicines with stock, past visits with vital signs and
prescriptions, lab orders, and 3 invoices with payments.

---

## Configuration

The API reads `src/main/resources/application.properties`, which imports `.env` from the same folder.
Each setting below can come from `.env` or from the environment.

| Variable | Default | What it sets |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:1200/clinic_db` | database |
| `DB_USERNAME` / `DB_PASSWORD` | `clinic_user` / none | database login |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:1201/realms/clinic` | who issues the tokens the API accepts |
| `API_AUDIENCE` | `clinic-api` | the audience tokens must name |
| `KEYCLOAK_SERVER_URL` / `KEYCLOAK_REALM` | `http://localhost:1201` / `clinic` | Keycloak for the admin API |
| `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` | `private-app` / none | the API's own Keycloak client |
| `PASSWORD_ENDPOINTS` | `true` (`false` in `prod`) | the password endpoints under `/auth` |
| `FRONTEND_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | CORS |
| `FRONTEND_LOGIN_URL` / `FRONTEND_CLIENT_ID` | `http://localhost:5173/login` / `cms-app` | where invite and reset emails lead back to |
| `CLINIC_NAME` / `CLINIC_ADDRESS` / `CLINIC_PHONE` | `CMS Clinic` / empty / empty | the heading of printed documents |
| `CLINIC_TIME_ZONE` | `Asia/Bangkok` | the clinic's day, e.g. for check-in |
| `BILLING_CURRENCY` / `CONSULTATION_FEE` | `THB` / `250.00` | invoices |

The API listens on port 1300. `SPRING_PROFILES_ACTIVE=prod` turns on the production profile.

`deployment/.env` serves Docker Compose and the scripts: `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`,
`POSTGRES_DB`, `KC_ADMIN_USERNAME`, `KC_ADMIN_PASSWORD`, and optionally `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET`. Both `.env` files are ignored by git; only the `.env.example` files are
committed.

---

## Testing

```bash
bash ./gradlew test
```

| Kind | Classes | What they show |
|---|---|---|
| Service unit tests (Mockito) | `VisitServiceImplTest`, `InvoiceServiceImplTest`, `PaymentServiceImplTest`, `PrescriptionServiceImplTest`, `CareScopeTest`, `WelcomeTest`, `StaffServiceImplTest`, `ClinicDirectoryServiceTest` | the business rules above, care scope, newcomers becoming patients, staff invites asking for two-factor |
| Security slices (`@WebMvcTest` with the real `SecurityConfig`) | `BillingSecurityTest`, `ConsultationSecurityTest`, `PublicAccessSecurityTest` | which permission opens which endpoint, and what is public |
| PDF rendering | `PrescriptionPdfRendererTest`, `ReceiptPdfRendererTest` | the documents render, also with data missing |
| Configuration | `PasswordEndpointsTest` | password endpoints on by default, off with the flag and in `prod` |
| Against the database (`@SpringBootTest`) | `CmsApplicationTests`, `CareScopeQueryTest` | the app starts; care-scope queries in real SQL (rolled back) |

The two `@SpringBootTest` classes use the development database, so start the containers and keep
`src/main/resources/.env` in place before running the tests.

### Postman collection

`postman/cms-all-roles.postman_collection.json` runs the API end to end for all seven roles against
the running stack: one clinic visit from booking to payment, a permission matrix of every role
against every permission, ownership checks (patients and doctors reach only their own data), and
signing in and sessions.

Before running: `keycloak_setup.sh` and `seed.sh` have run, and the API is up on port 1300 without
the `prod` profile. Run the folders in order, since later requests use ids saved by earlier ones:
in Postman with the Collection Runner, or from the command line:

```bash
npx newman@6.2.1 run postman/cms-all-roles.postman_collection.json
```

About signing in, it checks that:

- each login's token carries exactly its role's permissions and names the API as its audience
- visitors read the clinic directory and whether Google is offered without a login, while
  `/auth/welcome` needs one and leaves patients and staff as they are
- a password change, refresh and logout work, and a refresh token works once: replaying the
  replaced one is refused

Good to know:

- The logins use the password endpoints, so the collection runs against development only.
- Staff log in with the `staffPassword` variable. If a seeded account's password has been changed
  (on Keycloak's page, say), its login fails with 401, and so does everything that role does after
  it, along with requests needing the ids it would have created. Reset the password in the Keycloak
  admin console (*Temporary* off) or update the variable.
- Staff invited through `POST /api/v1/staff` must set up two-factor sign-in first, so they cannot
  log in here until they have.
- Each run registers a new patient, books the next free slot, registers one walk-in and adds one
  medicine (discontinued again at the end). No emails are sent.

---

## Deployment folder

| Path | What it is |
|---|---|
| `docker-compose.yml` | Postgres 17 (`clinic-db`, port 1200), Keycloak (`keycloak-clinic-db`, port 1201, `start-dev`), Mailpit (port 1202); on the external network `clinic-net` |
| `init_db/init_db.sql` | creates `clinic_db` and `keycloak_clinic_db` on the first start of Postgres |
| `keycloak_setup.sh` | sets up the realm and can be run again safely: email, sign-up and password reset, the login theme (once Keycloak has loaded it), token and session lifetimes, roles and permissions, the patient group, both clients, the audience, the service account's rights, Google, and optionally staff two-factor. Flags: `PASSWORD_LOGIN`, `STAFF_TWO_FACTOR`, `API_AUDIENCE`, `LOGIN_THEME`, `SMTP_HOST`, `KEYCLOAK_URL` |
| `seed.sh`, `seed.sql` | demo data and one login per role (skipped once loaded) |
| `backup.sh` | `pg_dump` of both databases into `backups/` (ignored by git) |
| `keycloak/login-theme/` | the sign-in pages (sign-in, sign-up, passwords, two-factor, notices), a Keycloakify project in the clinic's teal design; `npm run build-keycloak-theme` makes `keycloak/providers/spring-clinic-theme.jar` (needs Maven), see its README |
| `keycloak/clinic-theme/`, `keycloak/build_theme.sh` | the branded invite and reset emails; the script packs them into `keycloak/providers/clinic-theme.jar`, which Keycloak loads on restart |
| `keycloak_setup.sql` | an older SQL version of the realm set-up, replaced by `keycloak_setup.sh` |

---

## Production checklist

```bash
PASSWORD_LOGIN=false STAFF_TWO_FACTOR=true ./deployment/keycloak_setup.sh   # no password grant, staff 2FA
SPRING_PROFILES_ACTIVE=prod                                                 # password endpoints off
```

- Serve the web app, the API and Keycloak over HTTPS only (TLS at a reverse proxy, HTTP
  redirected), with Keycloak's `KC_HOSTNAME` set to its public `https://` address and Keycloak run
  with `start`, not `start-dev`. Keep the realm's *Require SSL* at *external requests* or stricter.
- Put the production web address in `FRONTEND_ORIGINS` in `keycloak_setup.sh` and in the API's
  `FRONTEND_ALLOWED_ORIGINS` and `FRONTEND_LOGIN_URL`, and drop the `localhost` ones.
- Pin the Keycloak image to a version instead of `latest`, set a real mail server, and keep the
  `.env` files out of the image and the repository.
- The `prod` profile does not create tables: create the schema first (see [Known gaps](#known-gaps)).

---

## Known gaps

- **Not built yet:** lab orders and results, reports, and dispensing. Their entities or empty
  classes exist, but there are no endpoints, and prescribing does not take medicines out of stock.
- **No migrations.** Development lets Hibernate update the schema; production has nothing to create
  it. Flyway would close this.
- **No container for the API.** `deployment/spring/Dockerfile` is empty and Compose runs only
  Postgres, Keycloak and Mailpit.
- **Swagger UI has no *Authorize* button**, since no bearer security scheme is declared, so it lists
  the endpoints but cannot call the protected ones.
- **Time zones.** Free slots, the MRN year and patient ages use the server's clock rather than
  `CLINIC_TIME_ZONE`; run the server in the clinic's time zone until that is fixed.
- **No database guard against double booking.** The service refuses a taken slot, but two bookings
  at the same instant could both pass.
- `/actuator/health` is allowed in `SecurityConfig`, but Actuator is not a dependency, so it answers 404.
- The demo data uses other number formats (`MRN-000001`, `INV-2026-0001`) than the app generates.

---

## Development conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
  `refactor:`, `test:`, `docs:`.
- New code goes in its feature package: controller, service interface and `*ServiceImpl`,
  repository, DTO records, MapStruct mapper.
- Every new endpoint gets a rule in `SecurityConfig` (anything without one only needs a login) and
  a security slice test for it; ownership rules go in the service, with a unit test.
- Business failures throw `BusinessRuleException` or `ResourceNotFoundException`, never a bare
  `RuntimeException`, so the client gets a 400 or 404 with a message.
- Roles and permissions change in `deployment/keycloak_setup.sh` first, then in `SecurityConfig`.

---

## Author

*Your name — Student ID — Course — Institution*
*Supervisor: ...*
