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

/** Decides whether an incoming request should be handled by a given route. */
public interface RouteMatcher {
    /**
     * @param route   the route candidate
     * @param ctx     the current Javalin request context
     * @return {@code true} if this route should handle the request
     */
    boolean matches(RouteDefinition route, Context ctx);
}

