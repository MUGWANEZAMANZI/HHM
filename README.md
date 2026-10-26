# Hospital Health Management System (HHM)

A JavaFX desktop application for managing hospital records including patients, doctors,
appointments, prescriptions, medical inventory, and patient feedback.
Built with Java 25, JavaFX 21, JDBC, and MySQL 8.0.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java JDK | 25 | Must match the compiler target in `pom.xml` |
| Apache Maven | 3.6+ | Used for dependency management and build |
| MySQL Server | 8.0+ | Local instance on port 3306 |
| MySQL Workbench | 8.0 | Optional — for browsing data and running EXPLAIN |
| IntelliJ IDEA | 2024+ | Recommended IDE;  |

---

## Database Setup

### 1. Create the schema

Open MySQL Workbench (or the MySQL CLI) and run:

```sql
CREATE DATABASE hhm;
```

### 2. Run the SQL script

Execute the full `db.sql` file located in the project root. It creates all tables,
foreign key constraints, indexes, and inserts seed data including 610 patients.

**MySQL Workbench:** File → Open SQL Script → select `db.sql` → Execute (lightning bolt)

**MySQL CLI:**
```bash
mysql -u root -p < db.sql
```

### 3. Verify

```sql
USE hhm;
SELECT COUNT(*) FROM Patient;   -- should return 610
SELECT COUNT(*) FROM Doctors;   -- should return 10
```

---

## Database Connection

The connection is configured in `src/main/java/com/util`:

```java
private static final String URL      = "jdbc:mysql://localhost:3306/hhm";
private static final String USER     = "root";
private static final String PASSWORD = "root";
```

Change `USER` and `PASSWORD` to match your local MySQL credentials before running.

---

## Running the Application

### Option A — IntelliJ IDEA (recommended)

1. Open the project: **File → Open** → select the `HHM` folder
2. Wait for Maven to download dependencies (first run only)
3. Press **Ctrl+Alt+Y** to sync file changes if prompted
4. Run `src/main/java/com/hhm` → right-click → **Run 'Hhm'**

### Option B — Maven command line

```bash
mvn clean javafx:run
```

---

## Running the Performance Benchmark

The benchmark measures query speed under four conditions and writes results to
`benchmark_results.csv` in the project root.

1. Ensure MySQL is running and `hhm` database is populated
2. Open `src/main/java/benchmark/DBBenchmark.java` in IntelliJ
3. Right-click → **Run 'DBBenchmark.main()'**
4. Results print to the console and are saved to `benchmark_results.csv`

> **Note:** The benchmark temporarily drops and restores `idx_patient_name` on
> the `Patient` table. Do not interrupt it mid-run.

---

## Project Structure

```
src/main/java/
  benchmark/
    DBBenchmark.java          Performance measurement tool
  controller/
    HomeController.java       Sidebar navigation, live clock
    PatientController.java    Patient CRUD
    DoctorController.java     Doctor CRUD
    AppointmentController.java Booking, cancellation, rescheduling
    PrescriptionController.java Prescription management
    InventoryController.java  Medical inventory with low-stock filter
    FeedbackController.java   Patient feedback recording
  dao/
    PatientDAO.java           JDBC access + AppCache integration
    DoctorDAO.java
    AppointmentDAO.java       JOIN query returning AppointmentView DTO
    PrescriptionDAO.java
    MedicalInventoryDAO.java
    PatientFeedbackDAO.java
    PatientAddressDAO.java
    DepartmentDAO.java
  model/
    Patient.java  Doctor.java  Appointment.java  AppointmentView.java
    Prescription.java  MedicalInventory.java  PatientFeedback.java
    PatientAddress.java  Department.java  MedicalItem.java
    PrescriptionItem.java  User.java
  service/
    PatientService.java       Business rules for patient operations
    DoctorService.java        Blocks delete if active appointments exist
    AppointmentService.java   Validates booking dates, cancellation rules
    PrescriptionService.java  Verifies patient exists before issuing
    MedicalInventoryService.java  SKU uniqueness, FK-safe delete
    PatientFeedbackService.java   Validates feedback text and patient
    ServiceException.java     Checked exception for business rule violations
  util/
    DBConnection.java         Single-point JDBC connection factory
    AppCache.java             Static in-memory cache (HashMap) with invalidation

src/main/resources/hhm/
  home-view.fxml              Main window with sidebar navigation
  patient-view.fxml
  doctor-view.fxml
  appointment-view.fxml
  prescription-view.fxml
  inventory-view.fxml
  feedback-view.fxml
```

---

## Architecture

The application follows a strict three-layer separation:

```
Controller  →  reads UI fields, shows status messages and alerts
    ↓
Service     →  enforces business rules, throws ServiceException
    ↓
DAO         →  parameterised SQL via JDBC, integrates with AppCache
    ↓
MySQL       →  persistent storage
```

---

## Key Technical Features

### Caching (`util/AppCache.java`)
A static `HashMap<Class<?>, List<?>>` holds one cached list per entity type.
Every `DAO.findAll()` checks the cache before hitting the database.
Every `insert`, `update`, and `delete` calls `AppCache.invalidate(Type.class)`
so the next read fetches fresh data.

### N+1 Query Elimination
`PatientController` previously made one SQL call per patient to fetch the address
(610 patients = 611 queries on load). Fixed by loading all addresses once with
`PatientAddressDAO.findAll()` returning a `Map<Long, PatientAddress>` keyed by
patient ID, reducing the page load to two queries total.

### In-Memory Search
All `search()` and `findBy*()` methods in DAOs filter the cached list using
Java streams instead of issuing new SQL queries. This gives O(n) filtering
entirely in RAM after the first load.

### Indexes
Six indexes are defined on high-frequency columns:

```sql
idx_patient_name          ON Patient(Name)
idx_doctors_department    ON Doctors(departmentId)
idx_appointments_date     ON Appointments(appointmentDate)
idx_appointments_patient  ON Appointments(patientId)
idx_appointments_doctor   ON Appointments(doctorId)
idx_prescriptions_patient ON Prescriptions(patientId)
```

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JavaFX Controls | 21.0.6 | UI components |
| JavaFX FXML | 21.0.6 | FXML view loading |
| mysql-connector-j | 9.3.0 | JDBC driver for MySQL |
| ControlsFX | 11.2.1 | Extended JavaFX controls |
| FormsFX | 11.6.0 | Form binding utilities |
| ValidatorFX | 0.6.1 | Input validation support |
| BootstrapFX | 0.4.0 | CSS styling helpers |
| TilesFX | 21.0.9 | Dashboard tile components |
| JUnit Jupiter | 5.12.1 | Unit testing (test scope) |

All dependencies are resolved automatically by Maven on first build.

---

## Sample Data

`db.sql` includes seed records for all tables:

| Table | Records |
|---|---|
| Departments | 10 |
| Patients | 610 (10 manual + 600 bulk-generated) |
| Patient Addresses | 612 |
| Doctors | 10 |
| Appointments | 12 |
| Prescriptions | 8 |
| MedicalInventory | 10 |
| PatientFeedback | 6 |
| PrescriptionItems | 9 |

---

## Performance Benchmarking & Optimization Analysis

### 1. Database Indexing & Query Latency

| Scenario | Operation | Rows Returned | Avg Latency (ms) | Execution Strategy |
| :--- | :--- | :---: | :---: | :--- |
| **No Index** | `SELECT *` (all rows) | 610 | `34.0 ms` | Full table scan |
| **No Index** | `Search LIKE 'Uwi%'` | 31 | `17.0 ms` | Full table scan |
| **With Index** | `SELECT *` (all rows) | 610 | `17.0 ms` | Clustered/covering index access |
| **With Index** | `Search LIKE 'Uwi%'` | 31 | `15.0 ms` | B-Tree index scan on `Name` column |

---

### 2. Application-Level Cache Performance (`PatientDAO`)

| Layer / State | Invocation | Rows Served | Latency (ms) | Data Source |
| :--- | :--- | :---: | :---: | :--- |
| **Cache Miss** | `PatientDAO.findAll()` | 610 | `21.0 ms` | Cache cleared — direct DB round-trip |
| **Cache Hit** | `PatientDAO.findAll()` | 610 | `< 1.0 ms` | Direct in-memory RAM lookup |

---

### 3. Summary & Speedup Metrics

* **Search Indexing Improvement:** **`1.13x`** faster query execution on prefixed wildcard filtering.
* **In-Memory Caching Improvement:** **`21.00x`** throughput acceleration via memory retrieval over database I/O.

## Author

- **Name:** MUGWANEZA MANZI Audace
