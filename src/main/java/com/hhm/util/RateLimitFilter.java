package com.hhm.util;

import com.hhm.service.RedisSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_HOUR = 1_000_000; // load-test ceiling (~4.2 hrs at 65 req/s)

    private final RedisSecurityService redisSecurityService;

    public RateLimitFilter(RedisSecurityService redisSecurityService) {
        this.redisSecurityService = redisSecurityService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);

        // Localhost is exempt — load tests from 127.0.0.1 / ::1 bypass the rate limiter
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (redisSecurityService.isRateLimited(ip, MAX_REQUESTS_PER_HOUR)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":429,\"message\":\"Too many requests — your IP is blocked for 24 hours\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
