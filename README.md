# Hospital Healthcare Management System — Module V

Spring Boot 4.1.1 / Java 25 backend with **async processing**, **concurrency management**, **Redis caching**, **JMeter load testing**, and **VisualVM profiling** — built on the Module IV secured REST + GraphQL API.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.1.1 (Web, JPA, Security, Async, Cache) |
| Language | Java 25 (Temurin-25.0.4.1) |
| Database | MySQL 8 (Docker) |
| Cache / Rate Limit | Redis 7 (Docker) |
| Connection Pool | HikariCP — pool=50 |
| Load Testing | Apache JMeter 5.6.3 |
| Profiling | VisualVM 2.2.2 |
| Auth | JWT (stateless) + Google OAuth2 + RBAC |

---

## Quick Start

**Prerequisites:** Java 25, Maven 3.9+, Docker

```bash
# Start MySQL + Redis
docker compose up -d

# Run the app (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

App starts at **http://localhost:8080** · Swagger UI at **http://localhost:8080/swagger-ui.html**

**Default admin:** `admin@hhm.rw` / `admin123`

---

## Module V — What Was Done

| Area | Implementation | Result |
|------|---------------|--------|
| Async | `@Async` + `CompletableFuture` on service layer | Non-blocking API responses |
| Thread Pool | `ThreadPoolTaskExecutor` core=10 / max=50 / queue=100 | Bounded concurrency |
| Caching | `@EnableCaching` + Redis `@Cacheable` on `PatientDAO` | **29× speedup** on repeated reads |
| DB Indexing | B-tree index on `patient(name)` | **1.92×** faster SELECT, **1.39×** faster LIKE |
| Connection Pool | HikariCP raised from 10 → 50 | **86× improvement** in avg response time |
| Hibernate Fix | `@JsonIgnoreProperties` on `Patient`, `Doctor`, `Department` | Eliminated all 500 errors |
| Load Testing | 7 JMeter runs; Runs 6 & 7 achieved **100% pass rate** (18,300 req, 0 errors) |

---

## Load Testing

```bash
# Flush Redis before each run
docker exec $(docker ps -qf "ancestor=redis") redis-cli FLUSHDB

# Run JMeter (non-GUI)
Remove-Item -Recurse -Force src/jmeter/report
jmeter -n -t src/jmeter/loadTesting.jmx -l src/jmeter/results/results.jtl -e -o src/jmeter/report
```

JMeter plan: 5 sequential thread groups — BCrypt stress (G1), Memory pressure (G2), Redis hammer (G3), AOP overhead (G4), 1000-user peak (G5).

---

## Seeded Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@hhm.rw` | `admin123` |
| Doctor (1–10) | `doctor1@hhm.rw` | `doctor123` |
| Nurse (1–30) | `nurse1@hhm.rw` | `nurse123` |
| Patient (1–100) | `patient1@hhm.rw` | `patient123` |

---

## Documentation

| Document | Description |
|----------|-------------|
| [`PERFORMANCE_REPORT.md`](PERFORMANCE_REPORT.md) | Full 7-run load test report with before/after metrics |
| [`docs/ASYNC_IMPLEMENTATION.md`](docs/ASYNC_IMPLEMENTATION.md) | `@Async`, `CompletableFuture`, thread pool config |
| [`docs/CONCURRENCY.md`](docs/CONCURRENCY.md) | Thread safety, concurrent collections, pool tuning |
| [`docs/ALGORITHMIC_OPTIMIZATIONS.md`](docs/ALGORITHMIC_OPTIMIZATIONS.md) | Caching (29×), indexing (1.92×), time complexity |

---

## Branch

`async-impl` — all Module V changes. Base: Module IV (`main`).

**Author:** MUGWANEZA MANZI Audace · audace.manzi@amalitechtraining.org
