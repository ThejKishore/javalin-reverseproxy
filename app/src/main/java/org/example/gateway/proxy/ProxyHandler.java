/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.proxy;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import org.example.gateway.audit.AuditGatewayFilter;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.filter.DefaultFilterChain;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.RouteDefinition;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Javalin {@link Handler} that processes every proxied HTTP request.
 *
 * <ol>
 *   <li>Matches the request against the {@link RouteRegistry}.</li>
 *   <li>Optionally wraps with {@link AuditGatewayFilter}.</li>
 *   <li>Executes the pre-built filter chain.</li>
 *   <li>Writes the upstream response back to the Javalin context.</li>
 * </ol>
 */
public class ProxyHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger(ProxyHandler.class);

    private final RouteRegistry registry;
    private final Jdbi jdbi;

    public ProxyHandler(RouteRegistry registry, Jdbi jdbi) {
        this.registry = registry;
        this.jdbi = jdbi;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        Optional<RouteDefinition> matched = registry.match(ctx);
        if (matched.isEmpty()) {
            throw new NotFoundResponse("No gateway route matched path: " + ctx.path());
        }
        RouteDefinition route = matched.get();
        FilterContext filterCtx = new FilterContext(ctx, route);
        FilterChain chain = buildChain(route, filterCtx);

        chain.proceed(filterCtx);
        filterCtx.applyToJavalinContext();
    }

    /** Prepends an AuditGatewayFilter when auditing is enabled for the route. */
    private FilterChain buildChain(RouteDefinition route, FilterContext filterCtx) {
        FilterChain routeChain = registry.getChain(route);
        if (route.isAuditEnabled() && jdbi != null) {
            List<GatewayFilter> auditWrapper = new ArrayList<>();
            auditWrapper.add(new AuditGatewayFilter(jdbi, route.getAuditStore()));
            // The audit filter wraps the inner chain via proceed
            return new DefaultFilterChain(List.of(
                    (c, next) -> {
                        AuditGatewayFilter audit = new AuditGatewayFilter(jdbi, route.getAuditStore());
                        audit.filter(c, routeChain::proceed);
                    }
            ));
        }
        return routeChain;
    }
}

