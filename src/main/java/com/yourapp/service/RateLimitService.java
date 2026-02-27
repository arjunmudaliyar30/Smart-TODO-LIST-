package com.yourapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for AI API calls.
 * Default: 20 requests per minute per user (configurable via AI_RATE_LIMIT_PER_MINUTE).
 NB: In-memory only — resets on restart; for multi-instance deployments, replace with Redis.
 */
@Service
public class RateLimitService {

    @Value("${ai.rate-limit.requests-per-minute:20}")
    private int requestsPerMinute;

    private static final long WINDOW_MS = 60_000L; // 1 minute

    // userId -> deque of request timestamps (milliseconds)
    private final ConcurrentHashMap<String, Deque<Long>> windowMap = new ConcurrentHashMap<>();

    /**
     * Check if the user is within their rate limit.
     * Throws 429 Too Many Requests if the limit is exceeded.
     *
     * @param userId the authenticated user's ID
     */
    public void checkLimit(String userId) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        Deque<Long> timestamps = windowMap.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps older than the window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= requestsPerMinute) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "AI rate limit exceeded. Maximum " + requestsPerMinute
                                + " requests per minute. Please wait and try again."
                );
            }

            timestamps.addLast(now);
        }
    }
}
