# Hospital Health Management System — Module II

Spring Boot REST + GraphQL API for managing hospital operations: patients, doctors,
departments, appointments, prescriptions, medical inventory, and patient feedback.

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

### 4. Switch profiles

```bash
# Development (default — debug logging, stack traces in errors)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production (info logging, errors hidden from consumers)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Using Swagger UI (REST API)

Swagger UI is auto-generated from the code — no manual documentation needed.

**URL:** http://localhost:8080/swagger-ui.html

### How to navigate

1. Open the URL in your browser. You will see a list of API tags:
   **Patient · Doctor · Appointment · Prescription · Inventory · Feedback**

2. Click a tag to expand all its endpoints. Each endpoint shows its HTTP method
   (GET / POST / PUT / PATCH / DELETE) and a short summary.

3. Click any endpoint row to expand its details — parameters, request body schema,
   and expected response codes are all shown.

### How to test an endpoint (Try it out)

1. Expand an endpoint (e.g. **POST /api/v1/patients**).
2. Click the **"Try it out"** button (top-right of the expanded card).
3. The request body field becomes editable. Swagger shows an example — modify it:
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
4. Click **"Execute"**.
5. Scroll down to see:
   - **Curl command** — copy it to run from terminal
   - **Request URL** — exact URL called
   - **Response body** — JSON returned by the API (wrapped in ApiResult)
   - **Response code** — 201 on success, 409 if email taken, 422 if fields invalid

### Response shape

Every response uses the same envelope:

```json
{
  "status": 200,
  "message": "Patients retrieved",
  "data": [ ... ]
}
```

Errors follow the same shape — no guessing about the format:

```json
{
  "status": 404,
  "message": "Patient with id 99 not found.",
  "data": null
}
```

### Pagination and sorting (Patients and Appointments)

Both GET /api/v1/patients and GET /api/v1/appointments support:

| Parameter | Default | Description |
|---|---|---|
| `page` | 0 | Page number (0-based) |
| `size` | 20 | Records per page |
| `sort` | name / date | Sort field |
| `search` | — | Filter by name/phone/email |

Example — page 2, 10 records, sorted by id:

```
GET /api/v1/patients?page=2&size=10&sort=id
```

---

## Using GraphiQL (GraphQL API)

GraphiQL is a browser-based IDE for writing and running GraphQL queries.

**URL:** http://localhost:8080/graphiql

### Interface layout

- **Left panel** — write your query or mutation here
- **Right panel** — response appears here after you run
- **Variables panel** — click "Variables" at the bottom to open it; use JSON variables
- **Documentation Explorer** — click the book icon (top-left) to browse the full schema
- **Run button** — the play button (or Ctrl + Enter) executes the query

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

**List all departments:**
```graphql
query {
  departments {
    id
    name
  }
}
```

**List all appointments:**
```graphql
query {
  appointments {
    id
    patientName
    doctorName
    departmentName
    appointmentDate
    status
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

**List low-stock inventory items:**
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

**Issue a prescription:**
```graphql
mutation {
  issuePrescription(patientId: 1, doctorId: 2, notes: "Take twice daily") {
    id
    status
    notes
  }
}
```

**Delete a patient:**
```graphql
mutation {
  deletePatient(id: 5)
}
```

### Using variables (cleaner for testing)

Instead of hardcoding values in the query, use variables:

**Query panel:**
```graphql
mutation RegisterPatient($name: String!, $phone: String!, $email: String!) {
  registerPatient(name: $name, phone: $phone, email: $email) {
    id
    name
  }
}
```

**Variables panel (open by clicking "Variables" at the bottom):**
```json
{
  "name": "Carol Mukamana",
  "phone": "0722000002",
  "email": "carol@example.com"
}
```

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v1/patients | List patients (search, sort, page) |
| POST | /api/v1/patients | Register patient |
| PUT | /api/v1/patients/{id} | Update patient |
| DELETE | /api/v1/patients/{id} | Delete patient |
| GET | /api/v1/doctors | List doctors (search) |
| POST | /api/v1/doctors | Register doctor |
| PUT | /api/v1/doctors/{id} | Update doctor |
| DELETE | /api/v1/doctors/{id} | Delete doctor |
| GET | /api/v1/appointments | List appointments (search, status, sort, page) |
| POST | /api/v1/appointments | Book appointment |
| PUT | /api/v1/appointments/{id} | Reschedule |
| PATCH | /api/v1/appointments/{id}/cancel | Cancel |
| DELETE | /api/v1/appointments/{id} | Delete |
| GET | /api/v1/prescriptions | List prescriptions |
| POST | /api/v1/prescriptions | Issue prescription |
| PUT | /api/v1/prescriptions/{id} | Update |
| PATCH | /api/v1/prescriptions/{id}/cancel | Cancel |
| DELETE | /api/v1/prescriptions/{id} | Delete |
| GET | /api/v1/inventory | List inventory (search) |
| GET | /api/v1/inventory/low-stock | Low-stock items |
| POST | /api/v1/inventory | Add item |
| PUT | /api/v1/inventory/{id} | Update item |
| DELETE | /api/v1/inventory/{id} | Delete item |
| GET | /api/v1/feedback | List feedback (search) |
| POST | /api/v1/feedback | Submit feedback |
| DELETE | /api/v1/feedback/{id} | Delete feedback |

---

## Spring Profiles

| Profile | File | Use case |
|---|---|---|
| `dev` (default) | application-dev.properties | Local development, DEBUG logging, stack traces exposed |
| `prod` | application-prod.properties | Production, INFO logging, errors hidden from consumers |

Activate via: `spring.profiles.active=prod` in application.properties, or pass
`-Dspring-boot.run.profiles=prod` at startup.

---

## AOP Logging

Centralized aspect (`com.aop.LoggingAop`) intercepts both controller and service layers:

| Advice | Layer | What it logs |
|---|---|---|
| `@Before` | Controller | Method name + arguments |
| `@After` | Controller | Method exited (always — success or error) |
| `@AfterReturning` | Controller | Return value on success |
| `@AfterThrowing` | Controller | Exception message on failure |
| `@Around` | Controller | Elapsed time in milliseconds |
| `@Before` | Service | Service class + method name + arguments |
| `@AfterReturning` | Service | Return value on success (DEBUG level) |
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

## REST vs GraphQL — Performance Analysis

| Dimension | REST | GraphQL |
|---|---|---|
| **Data fetching** | Fixed response shape — client receives all fields defined by the endpoint, even unused ones (over-fetching) | Client specifies exact fields needed — no over-fetching |
| **Multiple resources** | Requires N separate HTTP calls (e.g. patient + appointments + prescriptions = 3 requests) | Single query fetches all related data in one round-trip |
| **Latency (single resource)** | ~5–15 ms per endpoint (Tomcat + controller overhead) | ~8–20 ms (GraphQL parsing + resolver overhead) |
| **Caching** | HTTP-level caching works well (GET is cacheable by URL) | POST-based — HTTP caching does not apply without client-side tools |
| **Error handling** | HTTP status codes (200, 404, 422, etc.) signal error type | Always returns 200; errors are in `errors[]` array inside the response body |
| **Documentation** | Swagger UI auto-generates from annotations — zero extra work | GraphiQL schema explorer auto-generates from .graphqls file |
| **Best for** | CRUD operations, public APIs, mobile clients with predictable data needs | Dashboards, complex joins across multiple entities, flexible frontends |

**Recommendation for HHM:** Use REST for simple CRUD (patient/doctor registration,
inventory updates). Use GraphQL for complex reads such as the appointment dashboard
that combines patient name, doctor name, department, and status in one query.

---

## Project Structure

```
src/main/java/
  com/
    aop/        LoggingAop.java          @Aspect — controller + service logging
    controller/ PatientController.java   REST controllers (@RestController)
                DoctorController.java
                AppointmentController.java
                PrescriptionController.java
                InventoryController.java
                FeedbackController.java
                GlobalExceptionHandler.java  @RestControllerAdvice — centralized errors
    dao/        PatientDAO.java          JdbcTemplate-based data access
                DoctorDAO.java
                AppointmentDAO.java
                PrescriptionDAO.java
                MedicalInventoryDAO.java
                PatientFeedbackDAO.java
                DepartmentDAO.java
                PatientAddressDAO.java
    dto/        PatientRequest.java      Validated request bodies (@Valid DTOs)
                DoctorRequest.java
                AppointmentRequest.java
                PrescriptionRequest.java
                InventoryRequest.java
                FeedbackRequest.java
                ApiResult.java           Generic response envelope {status, message, data}
    graphql/    PatientResolver.java     GraphQL resolvers (@QueryMapping / @MutationMapping)
                DoctorResolver.java
                AppointmentResolver.java
                PrescriptionResolver.java
                InventoryResolver.java
                FeedbackResolver.java
                DepartmentResolver.java
    hhm/        HhmApplication.java      @SpringBootApplication entry point
    model/      Patient.java             Domain models (with @ToString for AOP logs)
                Doctor.java
                Department.java
                Appointment.java
                AppointmentView.java
                Prescription.java
                PrescriptionItem.java
                PatientFeedback.java
                MedicalInventory.java
                User.java
    service/    PatientService.java      Business rules + ServiceException
                DoctorService.java
                AppointmentService.java
                PrescriptionService.java
                MedicalInventoryService.java
                PatientFeedbackService.java
                ServiceException.java    Typed exception (NOT_FOUND / CONFLICT / VALIDATION)
    validation/ FutureDateTime.java      Custom @Constraint annotation
                FutureDateTimeValidator.java

src/main/resources/
  graphql/
    schema.graphqls              GraphQL schema — types, Query, Mutation
  application.properties         Shared config + active profile
  application-dev.properties     Dev: local DB, DEBUG logging
  application-prod.properties    Prod: env-var DB, INFO logging
```

---

## Author

**MUGWANEZA MANZI Audace** — audace.manzi@amalitechtraining.org
