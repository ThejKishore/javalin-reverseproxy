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

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Matches the request path against the route's {@code pathPattern} as a regex. */
public class RegexRouteMatcher implements RouteMatcher {

    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    @Override
    public boolean matches(RouteDefinition route, Context ctx) {
        String regex = route.getPathPattern();
        if (regex == null || regex.isBlank()) return false;
        Pattern compiled = patternCache.computeIfAbsent(regex, Pattern::compile);
        return compiled.matcher(ctx.path()).matches();
    }
}

