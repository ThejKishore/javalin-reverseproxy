/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.api;

import io.javalin.http.Context;
import org.example.gateway.registry.RouteRegistry;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes liveness and readiness health check endpoints.
 *
 * <ul>
 *   <li>{@code GET /gateway/health}       — combined status (liveness + readiness)</li>
 *   <li>{@code GET /gateway/health/live}  — liveness probe (always 200 when JVM is up)</li>
 *   <li>{@code GET /gateway/health/ready} — readiness probe (200 when routes are loaded)</li>
 * </ul>
 */
public class HealthController {

    private final RouteRegistry registry;
    private final Instant startTime = Instant.now();

    public HealthController(RouteRegistry registry) {
        this.registry = registry;
    }

    public void health(Context ctx) {
        boolean ready = !registry.getRoutes().isEmpty();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ready ? "UP" : "DEGRADED");
        body.put("liveness", "UP");
        body.put("readiness", ready ? "UP" : "NO_ROUTES");
        body.put("activeRoutes", registry.getRoutes().size());
        body.put("startTime", startTime.toString());
        ctx.status(ready ? 200 : 503).json(body);
    }

    public void liveness(Context ctx) {
        ctx.json(Map.of("status", "UP", "timestamp", Instant.now().toString()));
    }

    public void readiness(Context ctx) {
        boolean ready = !registry.getRoutes().isEmpty();
        ctx.status(ready ? 200 : 503)
           .json(Map.of("status", ready ? "UP" : "NO_ROUTES",
                        "activeRoutes", registry.getRoutes().size()));
    }
}

