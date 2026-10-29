# Changelog

All notable changes to Hospital Health Management System are documented here.

---

## [3.0.0] — 2026-10-29

### Added

**Spring Data JPA — Repository Layer**
- `spring-boot-starter-data-jpa` added to `pom.xml`; `spring-boot-starter-jdbc` removed
- 11 `JpaRepository` interfaces created in `com.repository`:
  `PatientRepository`, `DoctorRepository`, `DepartmentRepository`, `AppointmentRepository`,
  `PrescriptionRepository`, `PrescriptionItemRepository`, `PatientFeedbackRepository`,
  `MedicalInventoryRepository`, `MedicalItemRepository`, `PatientAddressRepository`, `UserRepository`
- Derived query methods — Spring generates SQL from method name at startup (no SQL code):
  `findByEmail()`, `findByPatientId()`, `findByDoctorId()`, `findByStatus()`, `existsById()`,
  `findByNameContainingIgnoreCase()`, `deleteByPatientId()`, `deleteByPrescriptionId()`, etc.
- JPQL `@Query` — database-agnostic named queries referencing Java class/field names:
  - `PatientRepository.search()` — multi-field keyword search across name, email, phone
  - `AppointmentRepository.findByStatusSorted()` — appointments sorted by date descending
  - `MedicalItemRepository.deleteByInventoryId()` — bulk child delete (`@Modifying`)
- Native SQL `@Query(nativeQuery = true)` — raw MySQL queries for performance-critical paths:
  - `PatientRepository.findAllPaged()` — paginated patient list ordered by name
  - `AppointmentRepository.findAllWithDetails()` — 4-table JOIN returning patient name,
    doctor name, department name alongside appointment fields

**JPA Entities**
- `@Entity`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` added to all
  11 model classes: Patient, Doctor, Department, Appointment, Prescription, PrescriptionItem,
  PatientFeedback, MedicalInventory, MedicalItem, PatientAddress, User
- Hibernate default naming strategy — no `@Table` or custom `@Column(name=...)` used;
  Hibernate derives table/column names from class/field names automatically
- `AppointmentView` intentionally excluded — it is a JOIN projection DTO, not a DB table

**Transaction Management**
- `@Transactional` on all service write methods (register, update, remove, save, delete)
  — rolls back the entire unit if any step throws an exception
- `@Transactional(readOnly = true)` on all service read methods (getAll, search, findBy)
  — signals no writes, enables DB read optimizations
- Propagation: `REQUIRED` (default) — joins an existing transaction or creates a new one

**Spring Cache**
- `@EnableCaching` already on `HhmApplication` — activated
- `@Cacheable` on all service read methods — result stored in memory after first DB call
- `@CacheEvict(allEntries = true)` on all service write methods — cache cleared after writes
- Cache names: `"patients"`, `"doctors"`, `"appointments"`, `"prescriptions"`,
  `"inventory"`, `"inventory-lowstock"`, `"feedback"`
- Cache key strategies: `key = "#keyword"`, `key = "#patientId"`, `key = "#status"`

**Properties — JPA config**
- `spring.jpa.hibernate.ddl-auto=validate` (dev) — Hibernate validates schema on startup
- `spring.jpa.show-sql=true` (dev) — generated SQL printed to console
- `spring.jpa.properties.hibernate.format_sql=true` (dev) — SQL formatted for readability

### Changed

**Services — full JPA migration**
- All services now inject `JpaRepository` interfaces instead of `JdbcTemplate` DAOs
- `insert()` / `update()` / `delete()` replaced by `save()` / `existsById()` / `deleteById()`
- `findById()` now returns `Optional<T>` — null checks replaced by `.orElseThrow()`
  and `.isPresent()` throughout all services
- `PatientService.register()` — email uniqueness check uses `findByEmail().isPresent()`
  instead of `findAll().stream().anyMatch()` (single-row DB query vs full table scan)
- `PrescriptionService.cancel()` — fetches entity with `findById().orElseThrow()`,
  mutates `status`, then calls `save()` — no separate update SQL needed
- `AppointmentService.cancel()` — same pattern: fetch → mutate status → save
- `MedicalInventoryService.remove()` — still calls `medicalItemRepository.deleteByInventoryId()`
  before deleting parent (FK constraint intact, now via `@Modifying` JPQL)
- `DoctorService.remove()` — checks `findByDoctorId()` for scheduled appointments
  before allowing deletion (same business rule, new method name)
- `PatientFeedbackService.search()` — replaced nested stream join across two tables
  with single repo call `findByFeedbackContainingIgnoreCase(keyword)`
- `MedicalInventoryService.getLowStock()` — replaced `findLowStock()` SQL with
  `findAll().stream().filter(m -> m.getQuantityOnHand() <= m.getReorderLevel())`
- Field names `inventoryDAO`, `feedbackDAO`, `addressDAO` renamed to
  `inventoryRepository`, `feedbackRepository`, `addressRepository`

**Model classes**
- `@Column(nullable = false)` added to `Patient.name`, `Patient.address`
- `@Column(unique = true, nullable = false)` added to `Patient.phone`, `Patient.email`

### Removed

- `com.dao` package — all `JdbcTemplate`-based DAO classes replaced by JPA repositories:
  `PatientDAO`, `DoctorDAO`, `AppointmentDAO`, `PrescriptionDAO`, `MedicalInventoryDAO`,
  `PatientFeedbackDAO`, `DepartmentDAO`, `PatientAddressDAO`, `MedicalItemDAO`
- Manual SQL strings, `ResultSet` mappers, `PreparedStatement` blocks — all eliminated
- `GeneratedKeyHolder` / `KeyHolder` usage — `save()` returns the entity with its generated id

---

## [2.1.0] — 2026-10-27

### Added

**Infrastructure**
- `spring-boot-starter-validation` dependency added to `pom.xml`
- Spring profiles: `application-dev.properties` (DEBUG logging, stack traces, local MySQL) and
  `application-prod.properties` (INFO logging, errors hidden, env-var credentials)
- `spring.profiles.active=dev` set in `application.properties` so dev is the default

**Response structure**
- `com.dto.ApiResult<T>` — generic response envelope: every API response now has
  `{ status, message, data }` instead of bare strings or entities

**Exception handling**
- `hhmcontroller` — `@RestControllerAdvice` handling:
  - `ServiceException` → 404 / 409 / 422 based on its `Type` enum
  - `MethodArgumentNotValidException` → 422 with field-level messages joined by "; "
  - `Exception` → 500 (unexpected errors)
- All per-controller `try/catch` blocks removed

**Custom validation**
- `com.hhm.validation.FutureDateTime` — custom `@Constraint` annotation
- `com.hhm.validation.FutureDateTimeValidator` — validates ISO 8601 format AND future date
- `AppointmentRequest.appointmentDate` annotated with `@FutureDateTime`

**AOP improvements**
- `@After` and `@Around` advice on controller pointcut
- Second pointcut `serviceAspect()` targeting `com.service.*.*`
- `@Before`, `@AfterReturning`, `@AfterThrowing` on service layer

**Sorting and pagination**
- `GET /api/v1/patients` and `GET /api/v1/appointments` accept `?sort=`, `?page=`, `?size=`

**Documentation**
- `README.md` rewritten for Spring Boot
- `CHANGELOG.md` created
- `@ToString` (Lombok) added to all model classes; `User` uses `@ToString(exclude="password")`

### Fixed
- `@Valid` on controller parameters now enforces constraints (validation starter was absent)
- Appointment date parse error returns 422 via `@FutureDateTime` instead of generic 500

---

## [2.0.0] — 2026-10-20

### Added
- Spring Boot project initialized with Web, JDBC, GraphQL, Swagger, Lombok, AOP
- Full REST CRUD for Patient, Doctor, Appointment, Prescription, MedicalInventory, Feedback
- GraphQL schema (`schema.graphqls`) with all types, Query, and Mutation blocks
- GraphQL resolvers for all entities
- AOP `@Before`, `@AfterReturning`, `@AfterThrowing` on controller layer
- Swagger UI via springdoc-openapi
- DTO pattern with `@NotBlank`, `@NotNull`, `@Min` field validation
- `ServiceException.Type` enum (NOT_FOUND / CONFLICT / VALIDATION)
- `JdbcTemplate`-based DAOs replacing manual JDBC
- Constructor-based dependency injection throughout
