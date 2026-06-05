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
import org.example.gateway.config.GatewayConfig;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    private RouteDefinition sampleRoute() {
        RouteDefinition r = new RouteDefinition();
        r.setId("test-id");
        r.setName("Test Route");
        r.setPathPattern("/api/test");
        r.setEnabled(true);
        r.setTargets(List.of(new TargetDefinition("http://localhost:9090", 1)));
        return r;
    }

    private Javalin buildApp(AdminController admin) {
        return Javalin.create(cfg ->
                cfg.routes.apiBuilder(() ->
                        path("/gateway/admin", () -> {
                            get("/routes", admin::listRoutes);
                            post("/routes", admin::createRoute);
                            get("/routes/{id}", admin::getRoute);
                            put("/routes/{id}", admin::updateRoute);
                            delete("/routes/{id}", admin::deleteRoute);
                            patch("/routes/{id}/enable", admin::enableRoute);
                            patch("/routes/{id}/disable", admin::disableRoute);
                            post("/reload", admin::reload);
                        })
                )
        );
    }

    @Test
    void listRoutes_returnsEmptyArray_initially() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.get("/gateway/admin/routes");
            assertEquals(200, resp.code());
            assertTrue(resp.body().string().contains("[]"));
        });
    }

    @Test
    void listRoutes_returnsLoadedRoutes() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of(sampleRoute()));
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.get("/gateway/admin/routes");
            assertEquals(200, resp.code());
            assertTrue(resp.body().string().contains("test-id"));
        });
    }

    @Test
    void getRoute_notFound_returns404() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.get("/gateway/admin/routes/missing");
            assertEquals(404, resp.code());
        });
    }

    @Test
    void reload_returns200() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.post("/gateway/admin/reload", "");
            assertEquals(200, resp.code());
            assertTrue(resp.body().string().contains("reloaded"));
        });
    }

    @Test
    void createRoute_acceptsRouteDto_andReturns201() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of());
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        // RouteDto format (kebab-case @JsonProperty keys, no security-policy fields)
        String createJson = """
            {
              "id": "new-route",
              "name": "New Route",
              "path-pattern": "/api/new",
              "routing-type": "PATH",
              "enabled": true,
              "timeout-ms": 3000,
              "load-balancer-type": "ROUND_ROBIN",
              "targets": [
                {"url": "http://localhost:9091", "weight": 1}
              ]
            }
            """;

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.post("/gateway/admin/routes", createJson);
            assertEquals(201, resp.code());
            String body = resp.body().string();
            assertTrue(body.contains("new-route"));
            assertTrue(body.contains("New Route"));
        });
    }

    @Test
    void updateRoute_acceptsRouteDto_returns200() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of(sampleRoute()));
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        // RouteDto update body — no jwt/csrf/csp-policy; they are managed separately
        String updateJson = """
            {
              "name": "Updated Route",
              "path-pattern": "/api/test",
              "routing-type": "PATH",
              "enabled": true,
              "timeout-ms": 5000,
              "load-balancer-type": "ROUND_ROBIN",
              "targets": [
                {"url": "http://localhost:9090", "weight": 1}
              ]
            }
            """;

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var put = client.put("/gateway/admin/routes/test-id", updateJson);
            assertEquals(200, put.code());

            var get = client.get("/gateway/admin/routes/test-id");
            assertEquals(200, get.code());
            String body = get.body().string();
            assertTrue(body.contains("Updated Route"));
            assertTrue(body.contains("5000"));
        });
    }

    @Test
    void getRoute_returnsRouteDtoFormat() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of(sampleRoute()));
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var resp = client.get("/gateway/admin/routes/test-id");
            assertEquals(200, resp.code());
            String body = resp.body().string();
            // RouteDto uses kebab-case via @JsonProperty
            assertTrue(body.contains("\"path-pattern\""));
            assertTrue(body.contains("test-id"));
        });
    }
}
