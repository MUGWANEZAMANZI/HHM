package com.hhm.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class SecurityEventService {

    private static final String KEY_LOGIN_SUCCESS  = "security:login:success";
    private static final String KEY_LOGIN_FAILURE  = "security:login:failure";
    private static final String PREFIX_ENDPOINT    = "security:endpoint:";
    private static final String PREFIX_BLACKLIST   = "blacklist:token:";

    private final StringRedisTemplate redis;

    public SecurityEventService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordLoginSuccess(String email) {
        redis.opsForValue().increment(KEY_LOGIN_SUCCESS);
        redis.opsForValue().increment("security:login:user:" + email);
    }

    public void recordLoginFailure(String email) {
        redis.opsForValue().increment(KEY_LOGIN_FAILURE);
        redis.opsForValue().increment("security:login:failure:user:" + email);
    }

    public void recordEndpointAccess(String controller, String method) {
        redis.opsForValue().increment(PREFIX_ENDPOINT + controller + "." + method);
    }

    public Map<String, Object> buildReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        String successRaw = redis.opsForValue().get(KEY_LOGIN_SUCCESS);
        String failureRaw = redis.opsForValue().get(KEY_LOGIN_FAILURE);
        report.put("loginSuccess", successRaw != null ? Long.parseLong(successRaw) : 0L);
        report.put("loginFailure", failureRaw != null ? Long.parseLong(failureRaw) : 0L);

        Set<String> blacklistKeys = redis.keys(PREFIX_BLACKLIST + "*");
        report.put("activeBlacklistedTokens", blacklistKeys != null ? blacklistKeys.size() : 0);

        Map<String, Long> freq = new TreeMap<>();
        Set<String> endpointKeys = redis.keys(PREFIX_ENDPOINT + "*");
        if (endpointKeys != null) {
            for (String key : endpointKeys) {
                String val = redis.opsForValue().get(key);
                String endpoint = key.substring(PREFIX_ENDPOINT.length());
                freq.put(endpoint, val != null ? Long.parseLong(val) : 0L);
            }
        }
        report.put("endpointFrequency", freq);

        return report;
    }
}
