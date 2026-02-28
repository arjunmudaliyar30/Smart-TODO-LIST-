package com.yourapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health check endpoint.
 * GET /health → 200 "OK" (plain text)
 * Used by uptime monitors and load balancers.
 * No database or service dependencies - always returns OK if app is running.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping(produces = "text/plain")
    public String health() {
        return "OK";
    }
}
