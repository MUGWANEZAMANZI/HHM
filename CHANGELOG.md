# Changelog

All notable changes to HHM Spring Boot API (Module II) are documented here.

---

## [2.1.0] — 2026-10-27

### Added

**Infrastructure**
- `spring-boot-starter-validation` dependency added to `pom.xml` (was only a comment placeholder —
  `@Valid` on controller parameters had no effect before this)
- Spring profiles: `application-dev.properties` (DEBUG logging, stack traces, local MySQL) and
  `application-prod.properties` (INFO logging, errors hidden, env-var credentials)
- `spring.profiles.active=dev` set in `application.properties` so dev is the default

**Response structure**
- `com.dto.ApiResult<T>` — generic response envelope: every API response now has
  `{ status, message, data }` instead of bare strings or entities

**Exception handling**
- `com.controller.GlobalExceptionHandler` — `@RestControllerAdvice` that handles:
  - `ServiceException` → 404 / 409 / 422 based on its `Type` enum
  - `MethodArgumentNotValidException` → 422 with field-level messages joined by "; "
  - `Exception` → 500 (unexpected errors)
- All per-controller `try/catch` blocks removed — error handling is now centralized

**Custom validation**
- `com.validation.FutureDateTime` — custom `@Constraint` annotation
- `com.validation.FutureDateTimeValidator` — validates that a String field is:
  1. A valid ISO 8601 `LocalDateTime`
  2. Strictly after the current timestamp
- `AppointmentRequest.appointmentDate` now annotated with `@FutureDateTime`

**AOP improvements**
- `@After` advice on controller pointcut — fires after method exits regardless of outcome
- `@Around` advice on controller pointcut — measures and logs elapsed time in milliseconds
- Second pointcut `serviceAspect()` targeting `com.service.*.*`
- `@Before`, `@AfterReturning`, `@AfterThrowing` advices on service layer

**Sorting and pagination**
- `GET /api/v1/patients` now accepts `?sort=`, `?page=`, `?size=` query parameters
- `GET /api/v1/appointments` now accepts `?sort=`, `?page=`, `?size=` query parameters
- In-memory sort then subList — no SQL changes needed

**Documentation**
- `README.md` rewritten for Spring Boot: startup, Swagger UI walkthrough, GraphiQL walkthrough,
  REST API reference, profile guide, AOP log format, REST vs GraphQL analysis
- `CHANGELOG.md` created

**Model readability**
- `@ToString` (Lombok) added to all model classes (Patient, Doctor, Department, Appointment,
  AppointmentView, Prescription, PrescriptionItem, PatientFeedback, MedicalInventory,
  PatientAddress, MedicalItem)
- `User` uses `@ToString(exclude = "password")` — password never appears in logs

### Changed

- All controllers return `ApiResult<T>` on success instead of bare strings or entities
- `PatientController` — removed per-controller `try/catch`; added sort + pagination
- `DoctorController` — removed per-controller `try/catch`
- `AppointmentController` — removed per-controller `try/catch`; added sort + pagination;
  removed `catch (Exception e)` for parse error (now handled by `@FutureDateTime`)
- `PrescriptionController` — removed per-controller `try/catch`
- `FeedbackController` — removed per-controller `try/catch`
- `InventoryController` — removed per-controller `try/catch`
- `LoggingAop` now has two pointcuts (controller + service) and five advice types per pointcut

### Fixed

- `@Valid` on controller parameters now enforces constraints (validation starter was absent)
- Appointment date format error now returns 422 with a clear message via `@FutureDateTime`
  instead of a generic catch-all

---

## [2.0.0] — earlier

### Added
- Spring Boot project initialized with Web, JDBC, GraphQL, Swagger, Lombok, AOP
- Full REST CRUD for Patient, Doctor, Appointment, Prescription, MedicalInventory, Feedback
- GraphQL schema (`schema.graphqls`) with all types, Query, and Mutation blocks
- GraphQL resolvers for all entities
- AOP `@Before`, `@AfterReturning`, `@AfterThrowing` on controller layer
- Swagger UI via springdoc-openapi
- DTO pattern with `@NotBlank`, `@NotNull`, `@Min` field validation
- `ServiceException.Type` enum (NOT_FOUND / CONFLICT / VALIDATION) for typed error routing
- `JdbcTemplate`-based DAOs replacing manual JDBC connection
- `DAO.update()` and `DAO.delete()` return `int` (rows affected) — detected missing records
- Constructor-based dependency injection throughout
