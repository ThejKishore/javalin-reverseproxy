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

/**
 * Matches TRAFFIC_SPLIT routes whose {@code path-pattern} is a prefix of the
 * request path (same segment-boundary logic as {@link PathRouteMatcher}).
 *
 * <p>Traffic is then distributed across targets by the configured load-balancer
 * (e.g. WEIGHTED or HEADER), not by this matcher.
 */
public class TrafficSplitRouteMatcher implements RouteMatcher {

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        if (route.getRoutingType() != RoutingType.TRAFFIC_SPLIT) return false;
        if (route.getTargets() == null || route.getTargets().isEmpty()) return false;

        // Must also respect the path-pattern (prefix match at segment boundary)
        String pattern = route.getPathPattern();
        if (pattern == null || pattern.isBlank()) return true; // no path constraint
        String path = ctx.path();
        if (path.equals(pattern)) return true;
        String prefix = pattern.endsWith("/") ? pattern : pattern + "/";
        return path.startsWith(prefix);
    }
}

