/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.api;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private Javalin buildApp(HealthController health) {
        return Javalin.create(cfg ->
                cfg.routes.apiBuilder(() ->
                        path("/gateway/health", () -> {
                            get(health::health);
                            get("/live",  health::liveness);
                            get("/ready", health::readiness);
                        })
                )
        );
    }

    @Test
    void liveness_always200() {
        HealthController health = new HealthController(new RouteRegistry());
        JavalinTest.test(buildApp(health), (server, client) -> {
            var resp = client.get("/gateway/health/live");
            assertEquals(200, resp.code());
            assertTrue(resp.body().string().contains("UP"));
        });
    }

    @Test
    void readiness_503_whenNoRoutes() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());
        HealthController health = new HealthController(registry);
        JavalinTest.test(buildApp(health), (server, client) -> {
            var resp = client.get("/gateway/health/ready");
            assertEquals(503, resp.code());
        });
    }

    @Test
    void readiness_200_whenRoutesLoaded() {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition r = new RouteDefinition();
        r.setId("r1"); r.setName("Test"); r.setPathPattern("/test"); r.setEnabled(true);
        r.setTargets(List.of(new TargetDefinition("http://localhost:9090", 1)));
        registry.reload(List.of(r));
        HealthController health = new HealthController(registry);
        JavalinTest.test(buildApp(health), (server, client) -> {
            var resp = client.get("/gateway/health/ready");
            assertEquals(200, resp.code());
        });
    }

    @Test
    void health_includesActiveRouteCount() {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition r = new RouteDefinition();
        r.setId("r1"); r.setName("Test"); r.setPathPattern("/test"); r.setEnabled(true);
        r.setTargets(List.of(new TargetDefinition("http://localhost:9090", 1)));
        registry.reload(List.of(r));
        HealthController health = new HealthController(registry);
        JavalinTest.test(buildApp(health), (server, client) -> {
            var resp = client.get("/gateway/health");
            String body = resp.body().string();
            assertTrue(body.contains("activeRoutes"));
            assertTrue(body.contains("1"));
        });
    }
}

