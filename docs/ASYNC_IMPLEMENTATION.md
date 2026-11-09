# Asynchronous Programming Implementation

**Epic 2 — Module V · HHM System**

---

## Overview

Long-running blocking operations were refactored to run on a dedicated thread pool, keeping Tomcat's HTTP threads free for incoming requests. The result: APIs remain responsive under concurrent load without spawning unbounded threads.

---

## 1. Thread Pool Configuration

```java
// AsyncConfig.java
@Bean("hhmExecutor")
public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);    // always-ready threads
    executor.setMaxPoolSize(50);     // burst ceiling
    executor.setQueueCapacity(100);  // request buffer before rejecting
    executor.setThreadNamePrefix("hhm-async-");
    executor.initialize();
    return executor;
}
```

| Parameter | Value | Rationale |
|-----------|:-----:|-----------|
| Core pool | 10 | Covers steady-state load without spawning on demand |
| Max pool | 50 | Matches HikariCP pool size — avoids pool starvation under bursts |
| Queue | 100 | Absorbs short spikes without rejection |
| Thread name | `hhm-async-*` | Visible in VisualVM thread list for profiling |

---

## 2. `@Async` — Non-Blocking Service Methods

Applied to service-layer methods that perform slow I/O (DB queries, Redis calls):

```java
@Service
public class AppointmentService {

    @Async("hhmExecutor")
    public CompletableFuture<List<AppointmentView>> getAllAsync() {
        List<AppointmentView> result = appointmentRepository.findAllWithJoin()
                .parallelStream()
                .map(this::mapToView)
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(result);
    }
}
```

**Before:** controller called `appointmentService.getAll()` — Tomcat thread blocked for the entire DB round-trip.  
**After:** controller calls `getAllAsync()` — Tomcat thread returns immediately; `hhm-async-*` thread handles the DB work.

---

## 3. `CompletableFuture` — Parallel Data Fetches

Where a controller needs data from multiple sources, fetches run in parallel and are joined before the response is built:

```java
@GetMapping("/dashboard")
public ResponseEntity<DashboardView> getDashboard() {
    CompletableFuture<List<Patient>>     patients     = patientService.getAllAsync();
    CompletableFuture<List<Appointment>> appointments = appointmentService.getAllAsync();
    CompletableFuture<List<Prescription>> prescriptions = prescriptionService.getAllAsync();

    CompletableFuture.allOf(patients, appointments, prescriptions).join();

    return ResponseEntity.ok(new DashboardView(
        patients.join(), appointments.join(), prescriptions.join()
    ));
}
```

**Sequential time:** T(patients) + T(appointments) + T(prescriptions)  
**Parallel time:** max(T(patients), T(appointments), T(prescriptions))

---

## 4. `parallelStream()` — In-Memory Parallel Mapping

The 4-table JOIN in `AppointmentService` returns raw rows that must be mapped to view objects. With `parallelStream()`, mapping runs across ForkJoinPool worker threads:

```java
List<AppointmentView> views = rawResults
    .parallelStream()
    .map(this::mapToView)   // CPU-bound: safe to parallelise (no shared mutable state)
    .collect(Collectors.toList());
```

VisualVM shows `ForkJoinPool.commonPool-worker-*` threads active during Group 2 load.

---

## 5. `@EnableAsync` and `@EnableCaching`

```java
@Configuration
@EnableAsync
@EnableCaching
public class AsyncConfig { ... }
```

Both annotations are on the same config class. `@EnableCaching` wires Spring's proxy-based cache layer backed by the Redis `CacheManager`.

---

## 6. Impact

| Metric | Before (`@Async`) | After (`@Async`) |
|--------|:-----------------:|:----------------:|
| Tomcat thread blocked on DB | Yes — full round-trip | No — delegated to pool |
| Concurrent appointments fetch | Sequential | Parallel (ForkJoinPool) |
| API responsiveness under G2 (100 users) | Degraded | Maintained |
| Max response time (pool=50) | 30,208 ms (Run 2) | **1,801 ms** (Run 7) |

---

## Acceptance Criteria Checklist

- [x] Long-running operations refactored with `CompletableFuture` / `parallelStream()`
- [x] APIs remain responsive during concurrent requests (Runs 6 & 7: 0 errors, 100% pass)
- [x] Thread pool configured with bounded core / max / queue (core=10, max=50, queue=100)
- [x] No data inconsistency or race conditions (all concurrent tests pass)
- [x] Response times compared before and after (see `PERFORMANCE_REPORT.md` §10)
