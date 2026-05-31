package com.yourapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiter for authentication endpoints.
 * <ul>
 *   <li>POST /api/auth/register — 5 requests / hour / IP</li>
 *   <li>POST /api/auth/login    — 10 requests / hour / IP</li>
 * </ul>
 * In-memory sliding window; replace with Redis for multi-instance deployments.
 */
@Component
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 3_600_000L; // 1 hour

    private static final int REGISTER_LIMIT = 5;
    private static final int LOGIN_LIMIT     = 10;

    // key: "IP:endpoint" -> deque of request timestamps
    private final ConcurrentHashMap<String, Deque<Long>> windowMap = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path   = request.getRequestURI();
        String method = request.getMethod();
        return !"POST".equalsIgnoreCase(method)
                || (!path.endsWith("/api/auth/register") && !path.endsWith("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path  = request.getRequestURI();
        String ip    = resolveIp(request);
        int    limit = path.endsWith("/register") ? REGISTER_LIMIT : LOGIN_LIMIT;
        String key   = ip + ":" + (path.endsWith("/register") ? "register" : "login");

        if (isLimitExceeded(key, limit)) {
            log.warn("Auth rate limit hit: ip={} path={}", ip, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimitExceeded(String key, int limit) {
        long now    = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        Deque<Long> ts = windowMap.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (ts) {
            while (!ts.isEmpty() && ts.peekFirst() < cutoff) {
                ts.pollFirst();
            }
            if (ts.size() >= limit) return true;
            ts.addLast(now);
            return false;
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take the first IP in the chain (real client IP)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
