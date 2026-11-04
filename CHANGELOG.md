# Changelog

All notable changes to Hospital Health Management System are documented here.

---

## [4.0.0] — 2026-11-04

### Added

**Spring Security — JWT Authentication**
- `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `io.jsonwebtoken:jjwt` 0.12.x added to `pom.xml`
- `SecurityConfig` — `SecurityFilterChain` with CORS, CSRF disabled, session policy `IF_REQUIRED` (stateless for JWT, session for OAuth2 web flow)
- `JwtService` — token generation, validation, claim extraction; secret loaded from `${spring.security.jwt.secret}`
- `JwtAuthenticationFilter` — `OncePerRequestFilter` before `UsernamePasswordAuthenticationFilter`; reads Bearer token, validates against blacklist, sets `SecurityContext`; JWT takes priority over any existing session
- `BlackListService` — `ConcurrentHashMap<token, expiry>` for O(1) token revocation; passive cleanup on access
- `AuthController` — `POST /api/v1/auth/login` (issues JWT), `POST /api/v1/auth/logout` (blacklists token), `POST /api/v1/auth/register` (PATIENT only)
- Custom `AuthenticationEntryPoint` — `/api/**` and `/graphql/**` receive 401 JSON; all other paths redirect to `/login`

**Google OAuth2**
- `OAuth2UserSaveService` — `OAuth2UserService` implementation; persists new Google users on first login with `displayName` and `provider = "GOOGLE"`; re-uses existing account on subsequent logins
- `GoogleLoginController` — `GET /login` (renders login page), `GET /dashboard` (OAuth2 landing, null-checks principal to guard against unauthenticated access)
- Pending user flow — OAuth2 users saved with `user_type = null`; admin assigns role via `PUT /api/v1/admin/users/{id}/role`

**Role-Based Access Control (RBAC)**
- `@EnableMethodSecurity` on `SecurityConfig`
- `@PreAuthorize("hasAuthority('ROLE')")` on every REST controller method and every GraphQL resolver mutation/query
- `User.getAuthorities()` derives authority directly from the `user_type` JPA discriminator column — no separate roles table
- Owner-only checks using `authentication.principal.id == #id` (e.g. doctor updates own record only, admin updates own account only)
- `AdminController` — `GET /api/v1/admin/users`, `GET /api/v1/admin/admins`, `GET /api/v1/admin/users/pending`, `POST /api/v1/admin` (create admin), `PUT /api/v1/admin/{id}` (self-update), `PUT /api/v1/admin/users/{id}/role` (promote OAuth2 user), `DELETE /api/v1/admin/users/{id}`

**GraphQL Security**
- `@PreAuthorize` added to all GraphQL resolver mutations (AppointmentResolver, FeedbackResolver, InventoryResolver, PrescriptionResolver, DepartmentResolver, DoctorResolver, PatientResolver)
- `/graphql/**` added to `permitAll()` in `SecurityConfig`; method-level `@PreAuthorize` enforces authorization per operation
- `AuthGraphQLResolver` — `login`, `logout`, `register`, `me` queries via GraphQL
- `AdminGraphQLResolver` — `users`, `admins`, `pendingUsers` queries; `createAdmin`, `updateAdmin`, `assignRole`, `deleteUser` mutations
- GraphQL schema updated with `User` type, `AuthToken` type, `LoginInput`, `RegisterInput`, `AssignRoleInput`

**Custom GraphiQL UI**
- Built-in Spring Boot GraphiQL replaced with a custom Thymeleaf template at `/graphiql`
- JWT token input bar — paste token, press Enter or click Apply; token sent as `Authorization: Bearer` on every request
- Token persisted in `localStorage` across page refreshes
- Status badge: green `✓ token active` / grey `— no token`

**Data Seeder**
- `DataSeeder` (`CommandLineRunner`) seeds on first boot only (`userRepository.count() > 0` guard):
  - 1 Admin (`admin@hhm.rw` / `admin123`)
  - 10 Doctors (`doctor1–10@hhm.rw` / `doctor123`)
  - 30 Nurses (`nurse1–30@hhm.rw` / `nurse123`)
  - 2 Receptionists (`receptionist1–2@hhm.rw` / `recep123`)
  - 100 Patients (`patient1–100@hhm.rw` / `patient123`)
  - 10 Departments, 10 Medical Inventory items with categories
  - 150 Appointments (60 Completed, 40 Cancelled, 50 Scheduled)
  - 60 Feedback entries
  - 60 Prescriptions with 1–3 items each

**Security Tests**
- `SecurityAccessControlTest` — `@WebMvcTest` with `@Import(SecurityConfig.class)`; 85+ MockMvc tests covering every role combination across all endpoints
- Custom `principal()` helper injects `User` subclass instances with real `id` for owner-only `@PreAuthorize` checks
- `@MockitoBean` added for `OAuth2UserSaveService` and `ClientRegistrationRepository` (required by OAuth2 DSL in `SecurityConfig`)

**Spring Security Architecture diagram**
- Mermaid flowchart in `README.md` mapping filter chain, `ProviderManager`, `DaoAuthenticationProvider`, `OAuth2LoginAuthenticationProvider`, `BlackListService`, `JwtService`, and `User implements UserDetails`

### Changed

- `/dashboard` removed from `permitAll()` — unauthenticated users are redirected to `/login`
- `JwtAuthenticationFilter` — JWT now overwrites existing session-based authentication when a `Bearer` token is present; previously a live OAuth2 session blocked JWT resolution
- GraphQL schema expanded with admin/auth types, queries, and mutations to match the REST API surface

### Fixed

- `GET /dashboard` accessible after logout — fixed by removing it from `permitAll()` and adding null-check on `principal` in `GoogleLoginController`
- GraphiQL receiving HTML `302` redirect instead of JSON — fixed by adding `/graphql` to the `authenticationEntryPoint` API path check and to `permitAll()`
- 85 test failures — `SecurityConfig` constructor requires `OAuth2UserSaveService`; `oauth2Login()` DSL requires `ClientRegistrationRepository`; both added as `@MockitoBean`
- Receptionist/Patient token blocked by browser OAuth2 session in GraphiQL — fixed by making JWT always win over session when `Authorization: Bearer` header is present

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
