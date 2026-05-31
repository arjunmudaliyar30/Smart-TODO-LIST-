package com.yourapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health check endpoint.
 * GET /health      → 200 "OK" (plain text)  — for uptime bots
 * GET /api/health  → 200 "OK" (plain text)  — for Render health check
 * No database or service dependencies - always returns OK if app is running.
 */
@RestController
public class HealthController {

    @GetMapping(value = {"/health", "/api/health"}, produces = "text/plain")
    public String health() {
        return "OK";
    }
}
