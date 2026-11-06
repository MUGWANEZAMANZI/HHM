package com.hhm.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // Named pool used for parallel DB lookups and @Async notifications.
    // Core = 10: handles normal load without creating threads on every request.
    // Max = 50: bursts up under heavy concurrency before queueing.
    // Queue = 100: requests wait here if all 50 threads are busy, instead of being dropped.
    // Chosen rationale: hospital system expects ~20-30 concurrent booking users;
    // core=10 covers steady state, max=50 leaves headroom for spikes.
    @Bean(name = "dbLookupExecutor")
    public Executor dbLookupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("db-lookup-");
        executor.initialize();
        return executor;
    }

    // Wires dbLookupExecutor as the default for all @Async annotations without a qualifier.
    // Without this override, Spring falls back to SimpleAsyncTaskExecutor which creates
    // an unbounded new thread per task — ignoring the pool entirely.
    @Override
    public Executor getAsyncExecutor() {
        return dbLookupExecutor();
    }
}
