/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.routing;

import io.javalin.http.Context;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.RoutingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathRouteMatcherTest {

    @Mock Context ctx;
    PathRouteMatcher matcher = new PathRouteMatcher();

    private RouteDefinition route(String pattern) {
        RouteDefinition r = new RouteDefinition();
        r.setPathPattern(pattern);
        r.setRoutingType(RoutingType.PATH);
        return r;
    }

    @Test
    void exactMatch() {
        when(ctx.path()).thenReturn("/api/users");
        assertTrue(matcher.matches(route("/api/users"), ctx));
    }

    @Test
    void prefixMatch() {
        when(ctx.path()).thenReturn("/api/users/123");
        assertTrue(matcher.matches(route("/api/users"), ctx));
    }

    @Test
    void noMatch_differentPrefix() {
        when(ctx.path()).thenReturn("/api/orders");
        assertFalse(matcher.matches(route("/api/users"), ctx));
    }

    @Test
    void noMatch_partialSegment() {
        // /api/users-v2 should NOT match /api/users
        when(ctx.path()).thenReturn("/api/users-v2");
        assertFalse(matcher.matches(route("/api/users"), ctx));
    }

    @Test
    void nullPattern_doesNotMatch() {
        when(ctx.path()).thenReturn("/anything");
        assertFalse(matcher.matches(route(null), ctx));
    }
}

