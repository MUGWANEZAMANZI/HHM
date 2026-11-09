# Algorithmic and Data Access Optimizations

**Epic 4 — Module V · HHM System**

---

## Overview

Three independent optimizations were applied and benchmarked: Redis caching, B-tree database indexing, and HikariCP connection pool right-sizing. Each is quantified with before/after execution times.

---

## 1. Redis Caching — `@Cacheable`

### Implementation

```java
// PatientDAO.java
@Cacheable(value = "patients", key = "'all'")
public List<Patient> findAll() {
    return patientRepository.findAll();
}

@CacheEvict(value = "patients", allEntries = true)
public Patient save(Patient patient) {
    return patientRepository.save(patient);
}
```

```java
// RedisConfig.java (CacheManager)
RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(10))
    .serializeValuesWith(RedisSerializationContext.SerializationPair
        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
```

### How It Works

```
First request  → Cache MISS → query MySQL (610 rows) → store in Redis → return
Subsequent     → Cache HIT  → return from Redis RAM (no DB round-trip)
Write (save)   → @CacheEvict → invalidates "patients::all" key → next read re-fetches
```

### Benchmark (610 patient rows)

| Scenario | Avg (ms) | Complexity |
|----------|:--------:|:----------:|
| Cache miss (MySQL) | 29 | O(n) table scan |
| **Cache hit (Redis)** | **~0** | **O(1) key lookup** |
| **Speedup** | **29×** | — |

---

## 2. Database Indexing — B-tree on `patient.name`

### Implementation

```sql
CREATE INDEX idx_patient_name ON patient (name);
```

### Time Complexity

| Query type | No index | With index |
|------------|----------|------------|
| `SELECT *` (all rows) | O(n) full table scan | O(n) but fewer I/O pages (index hints optimizer) |
| `WHERE name LIKE 'John%'` | O(n) full scan | O(log n) B-tree descent + range scan |
| `WHERE name = 'exact'` | O(n) | O(log n) |

### Benchmark (610 rows, `benchmark_results.csv`)

| Operation | No Index (ms) | With Index (ms) | Speedup |
|-----------|:-------------:|:---------------:|:-------:|
| `SELECT *` (all rows) | 50 | **26** | **1.92×** |
| `WHERE name LIKE ?` | 32 | **23** | **1.39×** |

The improvement is modest at 610 rows because MySQL's buffer pool already fits the table in memory. At millions of rows, B-tree descent reduces disk I/O from O(n) pages to O(log n) pages — the gap grows significantly.

---

## 3. HikariCP Connection Pool — Right-Sizing

### The Problem

HikariCP default = 10 connections. Under 100+ concurrent users (Group 2), all 10 connections are occupied, new requests queue, the 20-second `connection-timeout` fires, and login fails → JWT not extracted → 401 cascade on all downstream requests.

### Configuration

```properties
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
```

### Sizing Formula

```
pool_size = max_concurrent_DB_threads × avg_query_time / target_response_time
          = 50 × 0.1s / 0.1s
          = 50
```

Matches the `ThreadPoolTaskExecutor` max (50) — every async thread can hold a connection without contention.

### Impact

| Metric | Pool = 10 (Run 2) | Pool = 50 (Run 3) | Gain |
|--------|:-----------------:|:-----------------:|:----:|
| Avg response | 12,574 ms | **147 ms** | **86×** |
| SocketTimeoutException | 6,762 | **0** | Eliminated |
| 401 (cascade) | 2,731 | **0** | Eliminated |
| Throughput | 15.7 req/s | **87.6 req/s** | **5.6×** |

---

## 4. Redis N+1 Scan — Known Bottleneck (Documented)

`SecurityEventService.buildReport()` performs:

```
1. KEYS rateLimit:blocked:*   → O(N) full keyspace scan
2. KEYS rateLimit:count:*     → second O(N) scan
3. For each key: GET key      → N individual round-trips
```

Under 30 concurrent users hammering `GET /api/v1/analysis/report` (Group 3), response times reach 391–473 ms.

**Current:** O(N) KEYS scan + N GETs = O(2N) Redis operations  
**Optimal fix (not yet applied):** replace `KEYS` with `SCAN` cursor + store aggregated counts in a Redis hash

```
SCAN 0 MATCH rateLimit:blocked:* COUNT 100  → O(N) but non-blocking
HGETALL rateLimit:aggregates                → O(1) for pre-aggregated counts
```

---

## 5. `ConcurrentHashMap` — O(1) In-Memory Lookup

Endpoint frequency counters use `ConcurrentHashMap` with `AtomicLong` values:

```java
endpointFrequency.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
```

| Structure | Lookup | Insert | Thread-safe |
|-----------|:------:|:------:|:-----------:|
| `ArrayList` | O(n) | O(1) | ❌ |
| `HashMap` | O(1) | O(1) | ❌ |
| **`ConcurrentHashMap`** | **O(1)** | **O(1)** | **✅** |

---

## Summary

| Optimization | Technique | Complexity Change | Measured Gain |
|-------------|-----------|:-----------------:|:-------------:|
| Redis cache | `@Cacheable` + TTL | O(n) → O(1) | **29×** |
| DB index | B-tree on `name` | O(n) → O(log n) | **1.39–1.92×** |
| HikariCP pool | Raise default 10 → 50 | Queue wait → direct | **86×** avg response |
| In-memory lookup | `ConcurrentHashMap` | O(n) → O(1) | Thread-safe O(1) |

Full before/after load test data: [`PERFORMANCE_REPORT.md`](../PERFORMANCE_REPORT.md)
