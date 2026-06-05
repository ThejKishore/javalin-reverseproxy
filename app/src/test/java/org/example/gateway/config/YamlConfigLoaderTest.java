/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import org.example.utilities.gateway.model.LoadBalancerType;
import org.example.utilities.gateway.model.RoutingType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigLoaderTest {

    @Test
    void loadsDefaultConfigFromClasspath() throws Exception {
        GatewayConfig config = new YamlConfigLoader().load();

        assertNotNull(config);
        assertEquals(8080, config.getPort());
        assertTrue(
            java.util.Set.of("yaml", "database", "eclipse-store-lcl", "eclipse-store-azure")
                .contains(config.getConfigSource()),
            "config-source should be one of the supported values"
        );
        assertFalse(config.getRoutes().isEmpty(), "at least one route should be defined");
    }

    @Test
    void firstRouteHasRequiredFields() throws Exception {
        GatewayConfig config = new YamlConfigLoader().load();
        var route = config.getRoutes().get(0);

        assertNotNull(route.getId());
        assertNotNull(route.getName());
        assertNotNull(route.getPathPattern());
        assertEquals(RoutingType.PATH, route.getRoutingType());
        assertEquals(LoadBalancerType.ROUND_ROBIN, route.getLoadBalancerType());
        assertFalse(route.getTargets().isEmpty(), "route must have at least one target");
        assertNotNull(route.getTargets().get(0).getUrl());
        // JWT policy is now global — verify at gateway level, not per-route
        assertNotNull(config.getJwtPolicy(), "global jwt-policy should be configured");
        assertTrue(config.getJwtPolicy().isEnabled(), "global JWT policy should be enabled");
    }

    @Test
    void datasourceDefaultsToH2() throws Exception {
        GatewayConfig config = new YamlConfigLoader().load();
        assertEquals("h2", config.getDatasource().getType());
        assertNotNull(config.getDatasource().getUrl());
    }
}

