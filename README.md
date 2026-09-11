# Clinic Management System — REST API

A backend service for running a small-to-mid-size medical clinic: patient records, appointment scheduling, consultations, prescriptions, pharmacy stock, lab orders, and billing. Built as a stateless REST API secured by Keycloak.

> Academic project. Not certified for clinical use and not HIPAA/GDPR compliant as-is.

---

## Table of contents

- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Features by layer](#features-by-layer)
- [Feature modules](#feature-modules)
- [Project structure](#project-structure)
- [Domain model](#domain-model)
- [Roles and permissions](#roles-and-permissions)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Testing](#testing)
- [Development conventions](#development-conventions)
- [Roadmap](#roadmap)

---

## Tech stack

| Layer | Technology | Notes |
|---|---|---|
| Language | Java 21 | Records, pattern matching, virtual threads available |
| Framework | Spring Boot 3.3.x | Web MVC, Validation, Actuator |
| Security | Spring Security 6 + Keycloak 25 | OAuth2 Resource Server, JWT bearer tokens |
| Persistence | Spring Data JPA / Hibernate 6 | `ddl-auto=validate` only |
| Database | PostgreSQL 16 | `NUMERIC(12,2)` for money, `uuid` primary keys |
| Migrations | Flyway | Versioned SQL under `db/migration` |
| Mapping | MapStruct 1.5 | Compile-time entity ↔ DTO mapping |
| Boilerplate | Lombok | Getters, builders, constructors |
| API docs | springdoc-openapi 2.x | Swagger UI at `/swagger-ui.html` |
| Testing | JUnit 5, Mockito, Testcontainers, REST Assured | Real Postgres in integration tests |
| Build | Maven | Wrapper committed (`./mvnw`) |
| Runtime | Docker + Docker Compose | API, Postgres, Keycloak |

---

## Architecture

Layered architecture, organised **package-by-feature** rather than package-by-layer. Each feature owns its full vertical slice, so a change to billing touches one directory instead of five.

```
HTTP request
    │
    ▼
┌─────────────────────────────────────────────┐
│  Controller        REST, validation, DTOs   │  ← no business logic
├─────────────────────────────────────────────┤
│  Service           business rules, @Transactional
├─────────────────────────────────────────────┤
│  Repository        Spring Data JPA          │
├─────────────────────────────────────────────┤
│  Entity            JPA domain model         │
└─────────────────────────────────────────────┘
    │
    ▼
PostgreSQL
```

Cross-cutting concerns (security, auditing, exception handling, OpenAPI config) live in `common/` and `config/` and are shared by every feature.

**Rules enforced across the codebase**

- JPA entities are never returned from a controller. Every endpoint speaks DTOs.
- Controllers contain no `if` statements about business rules — those belong in services.
- Repositories are never injected into controllers.
- Anything writing to more than one table is `@Transactional`.

---

## Features by layer

### Presentation layer (`controller`)

- REST endpoints versioned under `/api/v1`
- Request validation via Bean Validation (`@Valid`, `@NotBlank`, `@Past`, `@Positive`)
- Method-level authorisation with `@PreAuthorize("hasAuthority('ROLE_patient:read')")`
- Consistent pagination, sorting and filtering (`?page=0&size=20&sort=lastName,asc`)
- RFC 7807 `application/problem+json` error responses
- OpenAPI annotations driving generated Swagger docs

### Application / service layer (`service`)

- Transaction boundaries and orchestration across repositories
- Business rules, including:
  - appointment overlap detection per doctor
  - appointment status state machine (`SCHEDULED → CHECKED_IN → COMPLETED`, with `CANCELLED` / `NO_SHOW` as terminal exits)
  - invoice total calculation with price snapshotting
  - invoice status derived from payments, never set directly
  - FEFO (first-expiry-first-out) stock deduction on dispensing
- Domain exceptions (`ResourceNotFoundException`, `BusinessRuleException`) translated to HTTP status by the global handler

### Persistence layer (`repository`, `entity`)

- Spring Data JPA repositories with derived and `@Query` methods
- `@EntityGraph` / `join fetch` on list endpoints to avoid N+1 selects
- Optimistic locking (`@Version`) on inventory and invoice rows
- JPA auditing populating `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- Soft delete via `active` flag on reference data; hard delete restricted to admin

### Security layer (`config`, `common/security`)

- Stateless resource server validating Keycloak-issued JWTs
- `KeycloakRoleConverter` mapping `realm_access.roles` to Spring authorities
- `CurrentUserService` resolving the JWT `sub` claim to a local `staff_profile` row
- Two-tier model: fine-grained permissions referenced in code, composite job roles assigned to users

### Cross-cutting (`common`)

- `@RestControllerAdvice` global exception handler
- `BaseEntity` mapped superclass with audit columns
- `PageResponse<T>` wrapper for consistent paged payloads
- Custom validators (e.g. MRN format, appointment window)

---

## Feature modules

| Module | Responsibility |
|---|---|
| `patient` | Registration, MRN generation, demographics, allergies, search |
| `staff` | Staff profiles linked to Keycloak identities, doctor records, departments |
| `appointment` | Booking, rescheduling, cancellation, check-in, availability query |
| `visit` | Consultation records, vitals, diagnosis, ICD-10 coding, patient timeline |
| `prescription` | Prescriptions and line items issued from a visit |
| `pharmacy` | Medicine catalogue, batches, expiry tracking, dispensing, stock alerts |
| `laboratory` | Test catalogue, lab orders from a visit, result entry and flagging |
| `billing` | Invoices, line items, discounts, payments, outstanding balances |
| `report` | Revenue, patient volume, doctor utilisation, common diagnoses |

---

## Project structure

```
clinic-management-api/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
├── README.md
├── docs/
│   ├── erd.png
│   ├── architecture.png
│   └── clinic-api.postman_collection.json
└── src/
    ├── main/
    │   ├── java/com/example/clinic/
    │   │   ├── ClinicApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── KeycloakRoleConverter.java
    │   │   │   ├── JpaAuditingConfig.java
    │   │   │   ├── OpenApiConfig.java
    │   │   │   └── CorsConfig.java
    │   │   │
    │   │   ├── common/
    │   │   │   ├── entity/BaseEntity.java
    │   │   │   ├── dto/PageResponse.java
    │   │   │   ├── exception/
    │   │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   ├── ResourceNotFoundException.java
    │   │   │   │   ├── BusinessRuleException.java
    │   │   │   │   └── ErrorResponse.java
    │   │   │   ├── security/
    │   │   │   │   ├── CurrentUserService.java
    │   │   │   │   └── Permissions.java
    │   │   │   └── util/MrnGenerator.java
    │   │   │
    │   │   ├── patient/
    │   │   │   ├── Patient.java
    │   │   │   ├── PatientRepository.java
    │   │   │   ├── PatientService.java
    │   │   │   ├── PatientController.java
    │   │   │   ├── PatientMapper.java
    │   │   │   └── dto/
    │   │   │       ├── PatientCreateRequest.java
    │   │   │       ├── PatientUpdateRequest.java
    │   │   │       ├── PatientResponse.java
    │   │   │       └── PatientSummaryResponse.java
    │   │   │
    │   │   ├── staff/
    │   │   │   ├── StaffProfile.java
    │   │   │   ├── Doctor.java
    │   │   │   ├── Department.java
    │   │   │   ├── StaffType.java
    │   │   │   ├── StaffProfileRepository.java
    │   │   │   ├── DoctorRepository.java
    │   │   │   ├── DepartmentRepository.java
    │   │   │   ├── DoctorService.java
    │   │   │   ├── DoctorController.java
    │   │   │   ├── DepartmentController.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── appointment/
    │   │   │   ├── Appointment.java
    │   │   │   ├── AppointmentStatus.java
    │   │   │   ├── AppointmentRepository.java
    │   │   │   ├── AppointmentService.java
    │   │   │   ├── AvailabilityService.java
    │   │   │   ├── AppointmentController.java
    │   │   │   ├── AppointmentMapper.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── visit/
    │   │   │   ├── Visit.java
    │   │   │   ├── VitalSign.java
    │   │   │   ├── VisitStatus.java
    │   │   │   ├── VisitRepository.java
    │   │   │   ├── VitalSignRepository.java
    │   │   │   ├── VisitService.java
    │   │   │   ├── VisitController.java
    │   │   │   ├── VisitMapper.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── prescription/
    │   │   │   ├── Prescription.java
    │   │   │   ├── PrescriptionItem.java
    │   │   │   ├── PrescriptionRepository.java
    │   │   │   ├── PrescriptionService.java
    │   │   │   ├── PrescriptionController.java
    │   │   │   ├── PrescriptionMapper.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── pharmacy/
    │   │   │   ├── Medicine.java
    │   │   │   ├── MedicineBatch.java
    │   │   │   ├── MedicineRepository.java
    │   │   │   ├── MedicineBatchRepository.java
    │   │   │   ├── MedicineService.java
    │   │   │   ├── DispensingService.java
    │   │   │   ├── StockAlertService.java
    │   │   │   ├── MedicineController.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── laboratory/
    │   │   │   ├── LabTest.java
    │   │   │   ├── LabOrder.java
    │   │   │   ├── LabOrderItem.java
    │   │   │   ├── LabOrderStatus.java
    │   │   │   ├── LabOrderRepository.java
    │   │   │   ├── LabTestRepository.java
    │   │   │   ├── LabOrderService.java
    │   │   │   ├── LabOrderController.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── billing/
    │   │   │   ├── Invoice.java
    │   │   │   ├── InvoiceItem.java
    │   │   │   ├── Payment.java
    │   │   │   ├── InvoiceStatus.java
    │   │   │   ├── ItemType.java
    │   │   │   ├── PaymentMethod.java
    │   │   │   ├── InvoiceRepository.java
    │   │   │   ├── PaymentRepository.java
    │   │   │   ├── InvoiceService.java
    │   │   │   ├── PaymentService.java
    │   │   │   ├── InvoiceController.java
    │   │   │   └── dto/
    │   │   │
    │   │   └── report/
    │   │       ├── ReportService.java
    │   │       ├── ReportController.java
    │   │       └── dto/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__init_schema.sql
    │           ├── V2__staff_and_departments.sql
    │           ├── V3__appointments_and_visits.sql
    │           ├── V4__pharmacy_and_lab.sql
    │           ├── V5__billing.sql
    │           └── V6__seed_reference_data.sql
    │
    └── test/
        ├── java/com/example/clinic/
        │   ├── patient/PatientServiceTest.java
        │   ├── appointment/AppointmentOverlapTest.java
        │   ├── billing/InvoiceCalculationTest.java
        │   ├── pharmacy/FefoDispensingTest.java
        │   └── integration/
        │       ├── AbstractIntegrationTest.java
        │       ├── PatientApiIT.java
        │       └── SecurityIT.java
        └── resources/application-test.yml
```

---

## Domain model

Seventeen tables across five groups. Full ERD in `docs/erd.png`.

**Identity and staff** — `staff_profile`, `department`, `doctor`
**Patients and scheduling** — `patient`, `appointment`
**Clinical** — `visit`, `vital_sign`, `prescription`, `prescription_item`
**Pharmacy and lab** — `medicine`, `medicine_batch`, `lab_test`, `lab_order`, `lab_order_item`
**Billing** — `invoice`, `invoice_item`, `payment`

Key relationships:

- A patient has many visits; a visit produces at most one prescription and one invoice
- `appointment → visit` is optional on both sides — walk-ins have no appointment, cancelled appointments never become visits
- `staff_profile → doctor` is optional-to-one — every doctor is staff, but not every staff member is a doctor
- `invoice_item.reference_id` is a soft reference pointing at a prescription item, lab order item, or the visit itself, discriminated by `item_type`. Deliberately not a foreign key.

**Design notes**

- Identity lives in Keycloak. There is no password column anywhere in this database. `staff_profile.keycloak_id` holds the JWT `sub` claim.
- `invoice_item` snapshots `description` and `unit_price` at invoice time so historical invoices don't change when a price does.
- All money is `BigDecimal` in Java and `NUMERIC(12,2)` in Postgres.
- Enums are persisted with `@Enumerated(EnumType.STRING)`.

---

## Roles and permissions

Two tiers. Permissions are fine-grained and named in code; roles are Keycloak composite roles bundling them. Users are assigned only a job role.

**Permissions** — `patient:read`, `patient:write`, `patient:delete`, `appointment:read`, `appointment:write`, `appointment:cancel`, `visit:read`, `visit:write`, `vitals:write`, `prescription:read`, `prescription:write`, `medicine:read`, `medicine:write`, `medicine:dispense`, `lab:order`, `lab:read`, `lab:result`, `invoice:read`, `invoice:write`, `payment:record`, `report:view`, `staff:manage`

**Roles**

| Permission | ADMIN | DOCTOR | NURSE | RECEPTIONIST | PHARMACIST | LAB_TECH |
|---|:--:|:--:|:--:|:--:|:--:|:--:|
| patient:read | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| patient:write | ✓ | — | ✓ | ✓ | — | — |
| patient:delete | ✓ | — | — | — | — | — |
| appointment:read | ✓ | ✓ | ✓ | ✓ | — | — |
| appointment:write | ✓ | — | ✓ | ✓ | — | — |
| appointment:cancel | ✓ | ✓ | — | ✓ | — | — |
| visit:read | ✓ | ✓ | ✓ | — | — | — |
| visit:write | ✓ | ✓ | — | — | — | — |
| vitals:write | ✓ | ✓ | ✓ | — | — | — |
| prescription:read | ✓ | ✓ | ✓ | — | ✓ | — |
| prescription:write | ✓ | ✓ | — | — | — | — |
| medicine:read | ✓ | ✓ | ✓ | — | ✓ | — |
| medicine:write | ✓ | — | — | — | ✓ | — |
| medicine:dispense | ✓ | — | — | — | ✓ | — |
| lab:order | ✓ | ✓ | — | — | — | — |
| lab:read | ✓ | ✓ | ✓ | — | — | ✓ |
| lab:result | ✓ | — | — | — | — | ✓ |
| invoice:read | ✓ | — | — | ✓ | ✓ | — |
| invoice:write | ✓ | — | — | ✓ | — | — |
| payment:record | ✓ | — | — | ✓ | ✓ | — |
| report:view | ✓ | ✓ | — | — | — | — |
| staff:manage | ✓ | — | — | — | — | — |

The asymmetries are intentional: receptionists see invoices but not clinical notes, doctors prescribe but cannot alter stock, and only lab technicians may enter results.

---

## Getting started

### Prerequisites

- JDK 21
- Docker and Docker Compose
- Maven (or use the bundled `./mvnw`)

### Run everything

```bash
git clone <repo-url>
cd clinic-management-api
docker compose up -d        # Postgres + Keycloak
./mvnw spring-boot:run
```

| Service | URL |
|---|---|
| API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Keycloak admin | http://localhost:8080 (`admin` / `admin`) |
| Postgres | `localhost:5432`, db `clinic` |

### Keycloak setup

The realm is imported automatically from `docker/keycloak/clinic-realm.json`. To configure manually instead:

1. Create realm `clinic`
2. Create a client `clinic-api` — client authentication on, service accounts enabled
3. Create every permission as a **realm role**
4. Create the six job roles and attach permissions via *Action → Add associated roles*
5. Add an **audience mapper**: client scopes → `clinic-api-dedicated` → *Add mapper → By configuration → Audience*, included client audience `clinic-api`
6. Create users and assign a single job role each

Step 5 is not optional — without it Spring rejects every token on audience validation.

### Get a token

```bash
curl -X POST http://localhost:8080/realms/clinic/protocol/openid-connect/token \
  -d "client_id=clinic-api" \
  -d "client_secret=<secret>" \
  -d "username=dr.sokha" \
  -d "password=password" \
  -d "grant_type=password"
```

```bash
curl http://localhost:8081/api/v1/patients \
  -H "Authorization: Bearer <access_token>"
```

---

## Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clinic
    username: ${DB_USER:clinic}
    password: ${DB_PASSWORD:clinic}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://localhost:8080/realms/clinic}
```

`open-in-view: false` is deliberate — it surfaces lazy-loading mistakes at development time instead of hiding them behind an open session.

| Variable | Default | Purpose |
|---|---|---|
| `DB_USER` | `clinic` | Postgres user |
| `DB_PASSWORD` | `clinic` | Postgres password |
| `KEYCLOAK_ISSUER` | `http://localhost:8080/realms/clinic` | Token issuer URI |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile |

---

## API overview

All endpoints are prefixed `/api/v1` and require a bearer token except `/actuator/health` and the Swagger routes.

| Method | Endpoint | Permission |
|---|---|---|
| `GET` | `/patients?search=&page=&size=` | `patient:read` |
| `POST` | `/patients` | `patient:write` |
| `GET` | `/patients/{id}` | `patient:read` |
| `GET` | `/patients/{id}/timeline` | `visit:read` |
| `GET` | `/doctors/{id}/availability?date=` | `appointment:read` |
| `POST` | `/appointments` | `appointment:write` |
| `PATCH` | `/appointments/{id}/check-in` | `appointment:write` |
| `PATCH` | `/appointments/{id}/cancel` | `appointment:cancel` |
| `POST` | `/visits` | `visit:write` |
| `POST` | `/visits/{id}/vitals` | `vitals:write` |
| `POST` | `/visits/{id}/prescription` | `prescription:write` |
| `POST` | `/visits/{id}/lab-orders` | `lab:order` |
| `PATCH` | `/lab-orders/{id}/items/{itemId}/result` | `lab:result` |
| `POST` | `/medicines/{id}/dispense` | `medicine:dispense` |
| `GET` | `/medicines/alerts/low-stock` | `medicine:read` |
| `POST` | `/invoices/from-visit/{visitId}` | `invoice:write` |
| `POST` | `/invoices/{id}/payments` | `payment:record` |
| `GET` | `/reports/revenue?from=&to=` | `report:view` |

Errors follow RFC 7807:

```json
{
  "type": "https://api.clinic.local/errors/appointment-conflict",
  "title": "Appointment conflict",
  "status": 409,
  "detail": "Dr. Sokha already has an appointment from 09:00 to 09:30 on 2026-03-14.",
  "instance": "/api/v1/appointments"
}
```

---

## Testing

```bash
./mvnw test                  # unit tests
./mvnw verify                # unit + integration (starts Testcontainers)
./mvnw jacoco:report         # coverage → target/site/jacoco/index.html
```

- **Unit tests** cover the rules worth breaking: appointment overlap, invoice totals, FEFO dispensing, status transitions.
- **Integration tests** run against a real PostgreSQL container via Testcontainers — no H2, whose SQL dialect differs enough to hide bugs.
- **Security tests** use `@WithMockUser(authorities = ...)` to assert that 403s actually fire for every restricted endpoint.

---

## Development conventions

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) — `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
- Branches: `feature/<name>`, `fix/<name>`
- Never edit an applied Flyway migration; add a new versioned file
- Every new endpoint ships with a `@PreAuthorize` annotation and at least one test
- DTOs are Java records; entities are Lombok classes

---

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 0 | Project setup, Docker, Flyway | ☐ |
| 1 | Keycloak realm, security config, staff profiles | ☐ |
| 2 | Exception handling, auditing, pagination, OpenAPI | ☐ |
| 3 | Patients, departments, doctors | ☐ |
| 4 | Appointments, overlap rules, availability | ☐ |
| 5 | Visits, vitals, prescriptions | ☐ |
| 6 | Invoices and payments | ☐ |
| 7 | Pharmacy and laboratory | ☐ |
| 8 | Reports, tests, seed data, documentation | ☐ |

**Out of scope:** patient self-service portal, insurance claim adjudication, multi-branch tenancy, real-time notifications, telemedicine.

---

## Author

*Your name — Student ID — Course — Institution*
*Supervisor: ...*
