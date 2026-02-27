package com.yourapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logs every HTTP request: method, path, status code, and duration.
 * Sets a unique request-id and (where determinable) userId in MDC
 * so all downstream log entries share the same correlation context.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        // Populate MDC for all downstream log statements in this request thread
        MDC.put("requestId", requestId);

        // Propagate request-id to the client so it can correlate support tickets
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();

            if (status >= 500) {
                log.error("{} {} → {} ({}ms) [reqId={}]", method, uri, status, duration, requestId);
            } else if (status >= 400) {
                log.warn("{} {} → {} ({}ms) [reqId={}]", method, uri, status, duration, requestId);
            } else {
                log.info("{} {} → {} ({}ms) [reqId={}]", method, uri, status, duration, requestId);
            }

            MDC.clear();
        }
    }

    /** Skip logging for static assets to reduce noise. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/assets/")
                || path.equals("/favicon.ico");
    }
}
