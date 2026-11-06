package com.hhm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitTestService {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RedisSecurityService service;

    private static final String IP          = "192.168.1.1";
    private static final String BLOCKED_KEY = "rateLimit:blocked:" + IP;
    private static final String COUNT_KEY   = "rateLimit:count:"   + IP;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ─── rate limit — normal traffic ─────────────────────────────────────────

    @Test
    void request_notBlocked_underLimit_returnsFalse() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(50L);

        assertThat(service.isRateLimited(IP, 100)).isFalse();
    }

    @Test
    void request_exactlyAtLimit_returnsFalse() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(100L);

        assertThat(service.isRateLimited(IP, 100)).isFalse();
    }

    // ─── rate limit — first request in window ────────────────────────────────

    @Test
    void request_firstInWindow_setsOneHourTtl() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(1L);

        service.isRateLimited(IP, 100);

        then(redisTemplate).should().expire(COUNT_KEY, 1, TimeUnit.HOURS);
    }

    @Test
    void request_notFirstInWindow_doesNotResetTtl() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(55L);

        service.isRateLimited(IP, 100);

        then(redisTemplate).should(never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    // ─── rate limit — threshold crossed ──────────────────────────────────────

    @Test
    void request_exceedsLimit_setsOneDayBlockAndReturnsTrue() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(101L);

        boolean result = service.isRateLimited(IP, 100);

        assertThat(result).isTrue();
        then(valueOps).should().set(BLOCKED_KEY, "blocked", 1, TimeUnit.DAYS);
    }

    @Test
    void request_farExceedsLimit_stillSetsBlockOnce() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(false);
        given(valueOps.increment(COUNT_KEY)).willReturn(999L);

        boolean result = service.isRateLimited(IP, 100);

        assertThat(result).isTrue();
        then(valueOps).should().set(BLOCKED_KEY, "blocked", 1, TimeUnit.DAYS);
    }

    // ─── rate limit — already blocked ────────────────────────────────────────

    @Test
    void request_alreadyBlocked_returnsTrueImmediately() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(true);

        boolean result = service.isRateLimited(IP, 100);

        assertThat(result).isTrue();
        then(valueOps).should(never()).increment(anyString());
    }

    @Test
    void request_alreadyBlocked_doesNotResetBlockTtl() {
        given(redisTemplate.hasKey(BLOCKED_KEY)).willReturn(true);

        service.isRateLimited(IP, 100);

        then(valueOps).should(never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    // ─── token blacklisting ───────────────────────────────────────────────────

    @Test
    void blacklistToken_storesKeyWithRemainingTtl() {
        Duration ttl = Duration.ofHours(5);

        service.blacklistToken("jti-abc", ttl);

        then(valueOps).should().set("blacklist:token:jti-abc", "revoked", ttl);
    }

    @Test
    void isTokenBlacklisted_keyExists_returnsTrue() {
        given(redisTemplate.hasKey("blacklist:token:jti-abc")).willReturn(true);

        assertThat(service.isTokenBlacklisted("jti-abc")).isTrue();
    }

    @Test
    void isTokenBlacklisted_keyAbsent_returnsFalse() {
        given(redisTemplate.hasKey("blacklist:token:jti-abc")).willReturn(false);

        assertThat(service.isTokenBlacklisted("jti-abc")).isFalse();
    }
}
