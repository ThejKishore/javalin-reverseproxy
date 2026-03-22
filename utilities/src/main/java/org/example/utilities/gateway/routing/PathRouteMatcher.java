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

/**
 * Matches when the request path starts with the route's {@code pathPattern}.
 * Also handles exact matches (pattern equals path).
 */
public class PathRouteMatcher implements RouteMatcher {

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        String pattern = route.getPathPattern();
        if (pattern == null || pattern.isBlank()) return false;
        String path = ctx.path();
        // Exact match or prefix match (pattern must end at a segment boundary)
        if (path.equals(pattern)) return true;
        String prefix = pattern.endsWith("/") ? pattern : pattern + "/";
        return path.startsWith(prefix);
    }
}

