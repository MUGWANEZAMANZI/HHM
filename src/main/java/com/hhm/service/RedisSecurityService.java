package com.hhm.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSecurityService {

    private final StringRedisTemplate redisTemplate;

    public RedisSecurityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- TOKEN BLACKLISTING ---

    public void blacklistToken(String jti, Duration expirationTTL) {
        String key = "blacklist:token:" + jti;
        // Store with a TTL matching the token's remaining lifetime (e.g., 1 day)
        redisTemplate.opsForValue().set(key, "revoked", expirationTTL);
    }

    public boolean isTokenBlacklisted(String jti) {
        String key = "blacklist:token:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }


    // --- RATE LIMITING (Fixed Window Example) ---

    public boolean isRateLimited(String clientIp, int maxRequests) {
        String blockedKey = "rateLimit:blocked:" + clientIp;
        String countKey   = "rateLimit:count:"   + clientIp;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey))) return true;

        Long requests = redisTemplate.opsForValue().increment(countKey);
        if (requests != null && requests == 1) {
            redisTemplate.expire(countKey, 1, TimeUnit.HOURS);
        }
        if (requests != null && requests > maxRequests) {
            redisTemplate.opsForValue().set(blockedKey, "blocked", 1, TimeUnit.DAYS);
            return true;
        }
        return false;
    }
}
