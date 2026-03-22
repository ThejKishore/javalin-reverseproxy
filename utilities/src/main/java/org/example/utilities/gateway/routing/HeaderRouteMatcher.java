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
 * Matches when the request contains a header whose name and value match
 * the route's {@code headerMatchName} / {@code headerMatchValue} configuration.
 */
public class HeaderRouteMatcher implements RouteMatcher {

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        String headerName = route.getHeaderMatchName();
        String headerValue = route.getHeaderMatchValue();
        if (headerName == null || headerName.isBlank()) return false;
        String actual = ctx.header(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return actual != null;
        }
        return headerValue.equals(actual);
    }
}

