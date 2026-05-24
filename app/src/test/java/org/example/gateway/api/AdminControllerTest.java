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
    void updateRoute_acceptsJwtPolicy_fromJsonBody() {
        RouteRegistry registry = new RouteRegistry();
        registry.reload(List.of(sampleRoute()));
        AdminController admin = new AdminController(registry,
                new RouteLoader(new GatewayConfig(), registry, null), null);

        String updateJson = """
            {
              "name": "Test Route",
              "path-pattern": "/api/test",
              "routing-type": "PATH",
              "enabled": true,
              "timeout-ms": 5000,
              "load-balancer-type": "ROUND_ROBIN",
              "targets": [
                {"url": "http://localhost:9090", "weight": 1}
              ],
              "jwt-policy": {
                "enabled": true,
                "algorithm": "HS256",
                "secret-or-public-key": "test-secret-key-minimum-32-chars!!",
                "issuer": "",
                "audience": "",
                "required-claims": {},
                "exclude-paths": []
              }
            }
            """;

        JavalinTest.test(buildApp(admin), (server, client) -> {
            var put = client.put("/gateway/admin/routes/test-id", updateJson);
            assertEquals(200, put.code());

            var get = client.get("/gateway/admin/routes/test-id");
            assertEquals(200, get.code());
            String body = get.body().string();
            assertTrue(body.contains("\"jwt-policy\""));
            assertTrue(body.contains("\"enabled\":true"));
        });
    }
}

