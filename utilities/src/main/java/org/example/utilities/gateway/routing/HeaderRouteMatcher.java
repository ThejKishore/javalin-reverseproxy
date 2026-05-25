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
 * Matches when:
 * <ol>
 *   <li>The request path starts with the route's {@code path-pattern} (segment-boundary
 *       prefix match, same logic as {@link PathRouteMatcher}), AND</li>
 *   <li>The request contains the route's {@code headerMatchName} header with the
 *       expected {@code headerMatchValue}.</li>
 * </ol>
 *
 * <p>If {@code path-pattern} is blank the path check is skipped (match any path).
 * If {@code headerMatchValue} is blank, any non-null value for the header matches.
 */
public class HeaderRouteMatcher implements RouteMatcher {

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        // 1. Path-prefix check (optional — skipped if path-pattern is blank)
        String pattern = route.getPathPattern();
        if (pattern != null && !pattern.isBlank()) {
            String path = ctx.path();
            if (!path.equals(pattern)) {
                String prefix = pattern.endsWith("/") ? pattern : pattern + "/";
                if (!path.startsWith(prefix)) return false;
            }
        }

        // 2. Header check
        String headerName  = route.getHeaderMatchName();
        String headerValue = route.getHeaderMatchValue();
        if (headerName == null || headerName.isBlank()) return false;
        String actual = ctx.header(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return actual != null;
        }
        return headerValue.equals(actual);
    }
}

