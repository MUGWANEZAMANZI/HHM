# Concurrency and Thread Safety

**Epic 3 — Module V · HHM System**

---

## Overview

Three concurrency concerns were addressed: thread-safe in-memory data structures, bounded thread pool management, and atomic Redis operations for shared rate-limit state.

---

## 1. Thread-Safe Collections — `ConcurrentHashMap`

Replaced plain `HashMap` with `ConcurrentHashMap` for all in-memory lookup tables that are read by multiple request threads simultaneously:

```java
// SecurityEventService.java
private final ConcurrentHashMap<String, AtomicLong> endpointFrequency = new ConcurrentHashMap<>();

public void recordEndpointAccess(String endpoint) {
    endpointFrequency
        .computeIfAbsent(endpoint, k -> new AtomicLong(0))
        .incrementAndGet();   // lock-free atomic increment
}
```

**Why not `HashMap`:** `HashMap` is not thread-safe. Concurrent `put()` calls from multiple request threads can corrupt the internal bucket structure (infinite loops in Java < 8, `ConcurrentModificationException` in Java 8+).

**Why not `synchronized HashMap`:** Synchronization serialises every read and write. `ConcurrentHashMap` uses segment-level locking — reads are lock-free, writes lock only the affected bucket.

| Operation | `HashMap` | `synchronized HashMap` | `ConcurrentHashMap` |
|-----------|:---------:|:----------------------:|:-------------------:|
| Concurrent reads | ❌ unsafe | ✅ serialised | ✅ lock-free |
| Concurrent writes | ❌ unsafe | ✅ serialised | ✅ bucket-level lock |
| Throughput under load | — | Low | **High** |

---

## 2. `AtomicLong` — Lock-Free Counters

Login success/failure counters use `AtomicLong` instead of `long` or `synchronized int`:

```java
// SecurityEventService.java
private final AtomicLong loginSuccess = new AtomicLong(0);
private final AtomicLong loginFailure = new AtomicLong(0);

public void recordLoginSuccess() { loginSuccess.incrementAndGet(); }
public void recordLoginFailure() { loginFailure.incrementAndGet(); }
```

`incrementAndGet()` uses a CAS (Compare-And-Swap) CPU instruction — no lock acquisition, no context switching.

---

## 3. Thread Pool — Bounded Concurrency

The `ThreadPoolTaskExecutor` prevents unbounded thread creation under spike load:

```java
executor.setCorePoolSize(10);   // always-alive threads
executor.setMaxPoolSize(50);    // hard ceiling — excess tasks queue
executor.setQueueCapacity(100); // 100 tasks buffer before rejection
```

**Without bounds:** 1,000 concurrent requests → 1,000 threads → heap exhaustion → OutOfMemoryError.  
**With bounds:** 1,000 requests → 50 active threads + 100 queued → remainder rejected with `TaskRejectedException` (graceful degradation).

**Pool sizing formula:**

```
pool_size = max_concurrent_threads × avg_task_time_s / target_response_s
pool_size = 50 × 0.1 / 0.1 = 50
```

Matches HikariCP `maximum-pool-size=50` — no thread waits for a connection and no connection is wasted.

---

## 4. Redis — Atomic Rate Limiting

`RateLimitFilter` stores per-IP counters and block flags in Redis. Redis is single-threaded; all operations are inherently atomic:

```java
// RedisSecurityService.java
public boolean isRateLimited(String ip, int maxRequests) {
    String countKey   = "rateLimit:count:" + ip;
    String blockedKey = "rateLimit:blocked:" + ip;

    if (Boolean.TRUE.equals(redis.hasKey(blockedKey))) return true;

    Long count = redis.opsForValue().increment(countKey);  // atomic INCR
    if (count == 1) redis.expire(countKey, 1, TimeUnit.HOURS);

    if (count > maxRequests) {
        redis.opsForValue().set(blockedKey, "1", 24, TimeUnit.HOURS);
        return true;
    }
    return false;
}
```

No Java-level synchronization is needed — `INCR` is atomic at the Redis level even under 1,000 concurrent callers.

---

## 5. `@JsonIgnoreProperties` — Safe Hibernate Proxy Serialisation

Under concurrent load, Hibernate returned `Patient$HibernateProxy` objects to Jackson serialisation threads. Jackson's attempt to call `LazyInitializer` on those proxies from multiple threads simultaneously caused `ClassCastException`.

**Fix:** tell Jackson to ignore Hibernate-internal fields before serialisation begins:

```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Patient extends User { ... }
```

Applied to `Patient`, `Doctor`, `Department` — all three have `@ManyToOne(fetch = LAZY)` relationships.

**Result:** 7 samplers previously returning 100% HTTP 500 dropped to 0% errors (confirmed in Runs 5–7).

---

## 6. Executor vs Concurrency Results (JMeter)

| Group | Users | Thread Pool State | Outcome |
|-------|:-----:|-------------------|---------|
| G1 BCrypt | 50 | 50 threads active, all BCrypt | 0 errors, avg 557 ms |
| G2 Bulk Fetch | 100 | `hhm-async-*` pool + ForkJoin | 0 errors, avg 86–145 ms |
| G4 AOP | 150 | Pool saturates queue briefly | 0 errors, avg 290–530 ms |
| G5 Peak | 1,000 | Ramp 8.3/s, threads recycle fast | 0 errors, avg 25–50 ms |

---

## Acceptance Criteria Checklist

- [x] `ConcurrentHashMap` used for shared in-memory structures
- [x] `AtomicLong` for shared counters (no synchronization blocks)
- [x] Thread pool configured and justified (core=10, max=50, queue=100)
- [x] Redis atomic INCR for rate-limit counting — no race conditions
- [x] Thread safety verified: Runs 6 & 7 achieve 100% pass rate under 1,330 virtual users
- [x] CPU and memory monitored during stress testing (VisualVM — see `PERFORMANCE_REPORT.md` §9)
