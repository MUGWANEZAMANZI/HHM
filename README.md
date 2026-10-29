# Hospital Health Management System — Module III

Spring Boot REST + GraphQL API backed by **Spring Data JPA** for managing hospital operations:
patients, doctors, departments, appointments, prescriptions, medical inventory, and patient feedback.

Module III upgrades the persistence layer from raw `JdbcTemplate` SQL to full JPA repositories
with derived queries, JPQL, native SQL, transaction management, and Spring Cache.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 25 |
| Apache Maven | 3.6+ |
| MySQL Server | 8.0+ |
| IntelliJ IDEA | 2024+ (recommended) |

---

## Quick Start

### 1. Database setup

```sql
CREATE DATABASE hhm;
-- Then run db.sql to create tables and seed data
mysql -u root -p hhm < db.sql
```

> **Module III note:** Hibernate manages the schema automatically via `ddl-auto=update`.
> You no longer need to run `db.sql` manually — Hibernate creates and alters tables on startup.

### 2. Configure credentials

Edit `src/main/resources/application-dev.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

On first run, Hibernate will print the generated CREATE TABLE statements to the console
(visible because `spring.jpa.show-sql=true` is set in the dev profile).

### 4. Switch profiles

```bash
# Development (default — debug logging, show SQL, stack traces in errors)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production (info logging, SQL hidden, errors hidden from consumers)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

```bash
# Development (default — debug logging, stack traces in errors)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

## Spring Data JPA — Repository Layer

All database access goes through `JpaRepository` interfaces in `com.repository`.
Spring generates the SQL implementation at startup — no DAO classes, no SQL strings,
no ResultSet mapping.

### Repository interfaces

| Repository | Entity | Notable custom queries |
|---|---|---|
| `PatientRepository` | `Patient` | `search()` (JPQL multi-field), `findAllPaged()` (native SQL) |
| `DoctorRepository` | `Doctor` | `findAll(Pageable)` |
| `DepartmentRepository` | `Department` | `findByName()`, `existsByName()` |
| `AppointmentRepository` | `Appointment` | `findByStatusSorted()` (JPQL), `findAllWithDetails()` (native JOIN) |
| `PrescriptionRepository` | `Prescription` | `findByPatientId()`, `findByDoctorId()` |
| `PrescriptionItemRepository` | `PrescriptionItem` | `findByPrescriptionId()`, `deleteByPrescriptionId()` |
| `PatientFeedbackRepository` | `PatientFeedback` | `findByFeedbackContainingIgnoreCase()` |
| `MedicalInventoryRepository` | `MedicalInventory` | `findAll(Pageable)` |
| `MedicalItemRepository` | `MedicalItem` | `deleteByInventoryId()` (`@Modifying` JPQL) |
| `PatientAddressRepository` | `PatientAddress` | `findByPatientId()`, `deleteByPatientId()` |
| `UserRepository` | `User` | `findByEmail()`, `existsByEmail()` |

### Three query types in use

**1. Derived queries** — Spring reads the method name and generates SQL automatically:
```java
// No implementation needed — Spring generates:
// SELECT * FROM patient WHERE email = ?
Optional<Patient> findByEmail(String email);

// SELECT * FROM patient WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
List<Patient> findByNameContainingIgnoreCase(String name);
```

**2. JPQL `@Query`** — references Java class and field names, database-agnostic:
```java
// References class name "Patient" and field "name", "email", "phone"
@Query("SELECT p FROM Patient p WHERE " +
       "LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
       "LOWER(p.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
       "p.phone LIKE CONCAT('%', :kw, '%')")
List<Patient> search(@Param("kw") String keyword);
```

**3. Native SQL `@Query(nativeQuery = true)`** — raw MySQL, references actual table/column names:
```java
// Raw SQL — fastest, but tied to MySQL syntax
@Query(value = "SELECT * FROM patient ORDER BY name ASC",
       countQuery = "SELECT COUNT(*) FROM patient",
       nativeQuery = true)
Page<Patient> findAllPaged(Pageable pageable);
```

**4. `@Modifying` JPQL** — for bulk updates/deletes that bypass the entity lifecycle:
```java
@Modifying
@Transactional
@Query("DELETE FROM MedicalItem m WHERE m.inventoryId = :inventoryId")
void deleteByInventoryId(@Param("inventoryId") long inventoryId);
```

```json
{
  "status": 404,
  "message": "Patient with id 99 not found.",
  "data": null
}
```

## Transaction Management

Every service method is annotated with `@Transactional` so database operations are atomic.

### Rules applied

| Annotation | Used on | Effect |
|---|---|---|
| `@Transactional` | Write methods (save, delete, update) | If any step throws an exception, all DB changes in that method are rolled back |
| `@Transactional(readOnly = true)` | Read methods (getAll, search, findBy) | Tells the DB no writes will happen — allows read optimizations, no write lock |

### Example — atomic patient registration

```java
@Transactional   // if address save fails, patient row is also rolled back
public long register(...) throws ServiceException {
    Patient saved = patientRepository.save(p);          // step 1
    addressRepository.save(new PatientAddress(...));     // step 2 — rolled back if this throws
    return saved.getId();
}
```

### Propagation (default: REQUIRED)

Spring's default propagation `REQUIRED` means: if a transaction already exists, join it;
if not, create a new one. This ensures that a service method called from another service
participates in the same transaction rather than opening a new one independently.

### Rollback verification

To verify rollback during testing, trigger an intentional failure on step 2
(e.g. pass an invalid `patientId` to a nested save). The patient row inserted in step 1
will not appear in the database — Spring rolls back the whole unit.

---

## Caching

`@EnableCaching` is active on `HhmApplication`. Spring Cache stores results in memory
(simple map by default) so repeated read requests skip the database entirely.

### What is cached

| Cache name | Cached by | Evicted when |
|---|---|---|
| `"patients"` | `getAll()`, `search(keyword)` | `register()`, `update()`, `remove()` |
| `"doctors"` | `getAll()`, `search(keyword)` | `register()`, `update()`, `remove()` |
| `"appointments"` | `getAll()`, `findByStatus()`, `search()` | `book()`, `reschedule()`, `cancel()`, `remove()` |
| `"prescriptions"` | `getAll()`, `getByPatient(id)` | `issue()`, `update()`, `cancel()`, `remove()` |
| `"inventory"` | `getAll()`, `search(keyword)` | `add()`, `update()`, `remove()` |
| `"inventory-lowstock"` | `getLowStock()` | `add()`, `update()`, `remove()` |
| `"feedback"` | `getAll()`, `search(keyword)`, `getByPatient(id)` | `submit()`, `remove()` |

### How it works

```
First call  → cache miss → hits database → stores result in cache → returns data
Second call → cache hit  → returns cached result (0 ms DB time)
Write call  → @CacheEvict clears the cache → next read hits DB again (fresh data)
```

### Cache invalidation strategy

All write methods use `@CacheEvict(value = "...", allEntries = true)` — the entire cache
for that entity is cleared after any write. This is safe: reads are cheap, staleness is not.

- **Left panel** — write your query or mutation here
- **Right panel** — response appears here after you run
- **Variables panel** — click "Variables" at the bottom to open it; use JSON variables
- **Documentation Explorer** — click the book icon (top-left) to browse the full schema
- **Run button** — the play button (or Ctrl + Enter) executes the query

## Pagination and Sorting

All list endpoints support `Pageable` via `JpaRepository.findAll(Pageable pageable)`.

### Swagger UI usage

**List all patients:**
```graphql
query {
  patients {
    id
    name
    phone
    email
  }
}
```
GET /api/v1/patients?page=0&size=10&sort=name,asc
GET /api/v1/appointments?page=1&size=5&sort=appointmentDate,desc
```

| Parameter | Default | Description |
|---|---|---|
| `page` | 0 | Page number (0-indexed) |
| `size` | 20 | Records per page |
| `sort` | name | Field name + `,asc` or `,desc` |

### Response shape with pagination

```json
{
  "status": 200,
  "message": "Patients retrieved",
  "data": {
    "content": [ { "id": 1, "name": "Alice" }, ... ],
    "totalElements": 87,
    "totalPages": 9,
    "number": 0,
    "size": 10
  }
}
```

**Get one patient by ID:**
```graphql
query {
  patient(id: 1) {
    id
    name
    phone
    email
    address
  }
}
```

**List all doctors with their department ID:**
```graphql
query {
  doctors {
    id
    name
    departmentId
    licenseNumber
  }
}
```

## Using Swagger UI (REST API)

Swagger UI is auto-generated from annotations — no manual documentation needed.

**URL:** http://localhost:8080/swagger-ui.html

### How to test an endpoint

1. Open the URL → expand a tag (Patient, Doctor, Appointment, etc.)
2. Click an endpoint → click **"Try it out"**
3. Edit the request body and click **"Execute"**
4. See the response below — body, status code, and curl command

### Request body example — register patient

```json
{
  "name": "Alice Uwimana",
  "phone": "0781234567",
  "email": "alice@example.com",
  "address": "KG 5 Ave",
  "district": "Kigali",
  "sector": "Gasabo",
  "cell": "Bumbogo",
  "village": "Kagugu"
}
```

### Response envelope

Every response — success or error — uses the same shape:

```json
{
  "status": 201,
  "message": "Patient registered",
  "data": { "id": 7 }
}
```

```json
{
  "status": 409,
  "message": "A patient with email \"alice@example.com\" already exists.",
  "data": null
}
```

### Example mutations

**Register a patient:**
```graphql
mutation {
  registerPatient(name: "Bob Habimana", phone: "0789000001", email: "bob@example.com") {
    id
    name
  }
}
```

## Using GraphiQL (GraphQL API)

**URL:** http://localhost:8080/graphiql

### Example queries

**List all patients:**
```graphql
query {
  patients {
    id
    name
    phone
    email
  }
}
```

**Search patients by keyword:**
```graphql
query {
  patients(search: "Alice") {
    id
    name
    email
  }
}
```

**Filter appointments by status:**
```graphql
query {
  appointments(status: "Scheduled") {
    id
    patientName
    doctorName
    appointmentDate
  }
}
```

**Low-stock inventory:**
```graphql
query {
  lowStockItems {
    id
    name
    sku
    quantityOnHand
    reorderLevel
  }
}
```

### Example mutations

**Register a patient:**
```graphql
mutation {
  registerPatient(name: "Bob Habimana", phone: "0789000001", email: "bob@example.com") {
    id
    name
  }
}
```

**Book an appointment:**
```graphql
mutation {
  bookAppointment(
    patientId: 1,
    doctorId: 2,
    appointmentDate: "2027-03-10T10:00:00",
    status: "Scheduled"
  ) {
    id
    status
  }
}
```

**Delete a record:**
```graphql
mutation {
  deletePatient(id: 5)
}
```

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v1/patients | List patients (page, size, sort, search) |
| POST | /api/v1/patients | Register patient |
| PUT | /api/v1/patients/{id} | Update patient |
| DELETE | /api/v1/patients/{id} | Delete patient |
| GET | /api/v1/doctors | List doctors |
| POST | /api/v1/doctors | Register doctor |
| PUT | /api/v1/doctors/{id} | Update doctor |
| DELETE | /api/v1/doctors/{id} | Delete doctor (blocked if has scheduled appointments) |
| GET | /api/v1/appointments | List appointments (page, size, sort, status) |
| POST | /api/v1/appointments | Book appointment |
| PUT | /api/v1/appointments/{id} | Reschedule |
| PATCH | /api/v1/appointments/{id}/cancel | Cancel |
| DELETE | /api/v1/appointments/{id} | Delete |
| GET | /api/v1/prescriptions | List prescriptions |
| POST | /api/v1/prescriptions | Issue prescription |
| PUT | /api/v1/prescriptions/{id} | Update |
| PATCH | /api/v1/prescriptions/{id}/cancel | Cancel |
| DELETE | /api/v1/prescriptions/{id} | Delete |
| GET | /api/v1/inventory | List inventory (page, size) |
| GET | /api/v1/inventory/low-stock | Items at or below reorder level |
| POST | /api/v1/inventory | Add item |
| PUT | /api/v1/inventory/{id} | Update item |
| DELETE | /api/v1/inventory/{id} | Delete item (cascade deletes child MedicalItems) |
| GET | /api/v1/feedback | List feedback |
| POST | /api/v1/feedback | Submit feedback |
| DELETE | /api/v1/feedback/{id} | Delete feedback |

---

## Spring Profiles

| Profile | File | DDL auto | Logging | SQL printed |
|---|---|---|---|---|
| `dev` (default) | application-dev.properties | `update` | DEBUG | Yes |
| `test` | application-test.properties | `create-drop` | WARN | No |
| `prod` | application-prod.properties | `validate` | INFO | No |

`ddl-auto` values explained:
- `update` — Hibernate adds missing columns/tables, never drops existing data (safe for dev)
- `create-drop` — drops and recreates schema on every run (good for automated tests)
- `validate` — Hibernate checks schema matches entities, throws on mismatch, never alters (safe for prod)

---

## AOP Logging

Centralized aspect (`com.hhm.aop.LoggingAop`) intercepts both controller and service layers:

| Advice | Layer | What it logs |
|---|---|---|
| `@Before` | Controller | Method name + arguments |
| `@After` | Controller | Method exited (always — success or error) |
| `@AfterReturning` | Controller | Return value on success |
| `@AfterThrowing` | Controller | Exception message on failure |
| `@Around` | Controller | Elapsed time in milliseconds |
| `@Before` | Service | Service class + method name + arguments |
| `@AfterReturning` | Service | Return value (DEBUG level) |
| `@AfterThrowing` | Service | Business rule violations (WARN level) |

Log output example (dev profile):
```
[CTRL] --> bookAppointment | args: [AppointmentRequest(patientId=1, ...)]
[SVC]  --> AppointmentService.book | args: [Appointment(id=0, patientId=1, ...)]
[SVC]  OK  book | returned: null
[CTRL] OK  bookAppointment | returned: <201 CREATED ...>
[CTRL] TIME bookAppointment completed in 23 ms
[CTRL] <-- bookAppointment | exited (success or error)
```

---

## REST vs GraphQL — When to Use Each

| Dimension | REST | GraphQL |
|---|---|---|
| **Data shape** | Fixed — client gets all fields the endpoint returns | Flexible — client picks only the fields it needs |
| **Multiple resources** | N HTTP calls for N related entities | One query fetches all related data |
| **HTTP caching** | Works natively on GET (URL-based) | Does not apply — GraphQL uses POST |
| **Error format** | HTTP status codes (404, 409, 422, 500) | Always 200; errors in `errors[]` array |
| **Documentation** | Swagger UI auto-generated from annotations | GraphiQL schema explorer auto-generated |
| **Best for** | CRUD operations, mobile clients, public APIs | Dashboards, complex joins, flexible frontends |

**Recommendation for HHM:** Use REST for simple CRUD (register patient, add inventory).
Use GraphQL for complex reads like the appointment dashboard that joins patient name,
doctor name, and department in a single query.

---

## Project Structure

```
src/main/java/
  com/
    aop/        LoggingAop.java              @Aspect — controller + service logging
    controller/ PatientController.java       REST controllers (@RestController)
                DoctorController.java
                AppointmentController.java
                PrescriptionController.java
                InventoryController.java
                FeedbackController.java
                GlobalExceptionHandler.java  @RestControllerAdvice — centralized errors
    dto/        PatientRequest.java          Validated request bodies (@Valid DTOs)
                DoctorRequest.java
                AppointmentRequest.java
                PrescriptionRequest.java
                InventoryRequest.java
                FeedbackRequest.java
                ApiResult.java               Generic response envelope {status, message, data}
    graphql/    PatientResolver.java         GraphQL resolvers (@QueryMapping / @MutationMapping)
                DoctorResolver.java
                AppointmentResolver.java
                PrescriptionResolver.java
                InventoryResolver.java
                FeedbackResolver.java
                DepartmentResolver.java
    hhm/        HhmApplication.java          @SpringBootApplication + @EnableCaching + @EnableAspectJAutoProxy
    model/      Patient.java                 JPA entities (@Entity, @Id, @GeneratedValue)
                Doctor.java
                Department.java
                Appointment.java
                AppointmentView.java         JOIN projection DTO — not an entity
                Prescription.java
                PrescriptionItem.java
                PatientFeedback.java
                MedicalInventory.java
                MedicalItem.java
                PatientAddress.java
                User.java                    @ToString(exclude="password") — password never in logs
    repository/ PatientRepository.java       JpaRepository interfaces — derived + JPQL + native SQL
                DoctorRepository.java
                DepartmentRepository.java
                AppointmentRepository.java
                PrescriptionRepository.java
                PrescriptionItemRepository.java
                PatientFeedbackRepository.java
                MedicalInventoryRepository.java
                MedicalItemRepository.java
                PatientAddressRepository.java
                UserRepository.java
    service/    PatientService.java          Business rules + @Transactional + @Cacheable
                DoctorService.java
                AppointmentService.java
                PrescriptionService.java
                MedicalInventoryService.java
                PatientFeedbackService.java
                ServiceException.java        Typed exception (NOT_FOUND / CONFLICT / VALIDATION)
    validation/ FutureDateTime.java          Custom @Constraint annotation
                FutureDateTimeValidator.java

src/main/resources/
  graphql/
    schema.graphqls                GraphQL schema — types, Query, Mutation
  application.properties           Shared config + active profile (dev)
  application-dev.properties       Dev: local DB, DEBUG logging, ddl-auto=update
  application-prod.properties      Prod: env-var DB, INFO logging, ddl-auto=validate
  application-test.properties      Test: hhm_test DB, WARN logging, ddl-auto=create-drop
```

---

## Author

**MUGWANEZA MANZI Audace** — audace.manzi@amalitechtraining.org
