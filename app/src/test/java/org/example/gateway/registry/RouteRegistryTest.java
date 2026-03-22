/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.registry;

import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteRegistryTest {

    private RouteRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RouteRegistry();
    }

    private RouteDefinition makeRoute(String id, String pattern) {
        RouteDefinition r = new RouteDefinition();
        r.setId(id);
        r.setName("Test " + id);
        r.setPathPattern(pattern);
        r.setEnabled(true);
        r.setTargets(List.of(new TargetDefinition("http://localhost:9090", 1)));
        return r;
    }

    @Test
    void reload_loadsEnabledRoutes() {
        registry.reload(List.of(
                makeRoute("r1", "/api/a"),
                makeRoute("r2", "/api/b")));
        assertEquals(2, registry.getRoutes().size());
    }

    @Test
    void reload_skipsDisabledRoutes() {
        RouteDefinition disabled = makeRoute("r3", "/api/c");
        disabled.setEnabled(false);
        registry.reload(List.of(disabled));
        assertTrue(registry.getRoutes().isEmpty());
    }

    @Test
    void addOrUpdate_addsNewRoute() {
        registry.reload(List.of());
        registry.addOrUpdate(makeRoute("new", "/api/new"));
        assertEquals(1, registry.getRoutes().size());
    }

    @Test
    void addOrUpdate_updatesExistingRoute() {
        RouteDefinition original = makeRoute("r1", "/api/original");
        registry.reload(List.of(original));

        RouteDefinition updated = makeRoute("r1", "/api/updated");
        registry.addOrUpdate(updated);

        assertEquals(1, registry.getRoutes().size());
        assertEquals("/api/updated", registry.getRoutes().get(0).getPathPattern());
    }

    @Test
    void remove_deletesRoute() {
        registry.reload(List.of(makeRoute("r1", "/api/a")));
        registry.remove("r1");
        assertTrue(registry.getRoutes().isEmpty());
    }

    @Test
    void setEnabled_false_removesFromRegistry() {
        registry.reload(List.of(makeRoute("r1", "/api/a")));
        registry.setEnabled("r1", false);
        assertTrue(registry.getRoutes().isEmpty());
    }
}

