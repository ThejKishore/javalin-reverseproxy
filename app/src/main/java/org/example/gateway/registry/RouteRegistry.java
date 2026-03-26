/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.registry;

import io.javalin.http.Context;
import org.example.utilities.gateway.cache.CacheGatewayFilter;
import org.example.utilities.gateway.cache.CaffeineCache;
import org.example.utilities.gateway.circuitbreaker.CircuitBreakerFactory;
import org.example.utilities.gateway.circuitbreaker.CircuitBreakerGatewayFilter;
import org.example.utilities.gateway.filter.DefaultFilterChain;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.header.DedupeResponseHeadersFilter;
import org.example.utilities.gateway.header.HeaderMutationFilter;
import org.example.utilities.gateway.loadbalancer.*;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.RoutingType;
import org.example.utilities.gateway.proxy.ForwardedForFilter;
import org.example.utilities.gateway.proxy.OkHttpUpstreamClient;
import org.example.utilities.gateway.proxy.ProxyFilter;
import org.example.utilities.gateway.ratelimit.SlidingWindowRateLimiter;
import org.example.utilities.gateway.routing.*;
import org.example.utilities.gateway.tracing.TracingFilter;
import org.example.utilities.gateway.transform.TransformGatewayFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory store of active {@link RouteDefinition} instances.
 *
 * <p>A pre-built {@link FilterChain} is cached per route and rebuilt on each
 * {@link #reload(List)} call.  All operations are thread-safe.
 */
public class RouteRegistry {

    private static final Logger log = LoggerFactory.getLogger(RouteRegistry.class);

    /** All routes (enabled + disabled) – used by the admin management API. */
    private final ConcurrentHashMap<String, RouteDefinition> allRoutes = new ConcurrentHashMap<>();

    /** Only enabled routes, ordered for proxy matching. */
    private final CopyOnWriteArrayList<RouteDefinition> routes = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, FilterChain> chains = new ConcurrentHashMap<>();

    private final TracingFilter tracingFilter = new TracingFilter();
    private final ForwardedForFilter forwardedForFilter = new ForwardedForFilter();
    private final OkHttpUpstreamClient upstreamClient = new OkHttpUpstreamClient();

    private final PathRouteMatcher pathMatcher = new PathRouteMatcher();
    private final RegexRouteMatcher regexMatcher = new RegexRouteMatcher();
    private final HeaderRouteMatcher headerMatcher = new HeaderRouteMatcher();
    private final TrafficSplitRouteMatcher trafficSplitMatcher = new TrafficSplitRouteMatcher();

    /**
     * Replaces the current route list with {@code newRoutes} and rebuilds all
     * filter chains atomically.
     */
    public synchronized void reload(List<RouteDefinition> newRoutes) {
        allRoutes.clear();
        routes.clear();
        chains.clear();
        for (RouteDefinition r : newRoutes) {
            allRoutes.put(r.getId(), r);          // store every route for admin API
            if (r.isEnabled()) {
                routes.add(r);
                chains.put(r.getId(), buildChain(r));
                log.info("Registered route '{}' [{}] → {}", r.getName(), r.getPathPattern(), r.getTargets());
            } else {
                log.debug("Route '{}' is disabled – skipped from proxy matching", r.getId());
            }
        }
        log.info("RouteRegistry reloaded — {} active route(s), {} total", routes.size(), allRoutes.size());
    }

    /** Finds the first matching route for the incoming request. */
    public Optional<RouteDefinition> match(Context ctx) {
        for (RouteDefinition route : routes) {
            if (matches(route, ctx)) return Optional.of(route);
        }
        return Optional.empty();
    }

    /** Returns the pre-built filter chain for a route. */
    public FilterChain getChain(RouteDefinition route) {
        return chains.get(route.getId());
    }

    /**
     * Returns an unmodifiable snapshot of the <em>active</em> (enabled) routes.
     * Used internally for proxy matching.
     */
    public List<RouteDefinition> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * Returns an unmodifiable snapshot of <em>all</em> routes, including
     * disabled ones.  Used by the admin management API.
     */
    public List<RouteDefinition> getAllRoutes() {
        return List.copyOf(allRoutes.values());
    }

    // ── Dynamic management ───────────────────────────────────────────────────

    public synchronized void addOrUpdate(RouteDefinition route) {
        // Always update the master map
        allRoutes.put(route.getId(), route);

        // Remove from active list (will re-add below if enabled)
        routes.removeIf(r -> r.getId().equals(route.getId()));
        chains.remove(route.getId());

        if (route.isEnabled()) {
            routes.add(route);
            chains.put(route.getId(), buildChain(route));
            log.info("Route '{}' added/updated (active)", route.getId());
        } else {
            log.info("Route '{}' added/updated (disabled)", route.getId());
        }
    }

    public synchronized void remove(String routeId) {
        allRoutes.remove(routeId);
        routes.removeIf(r -> r.getId().equals(routeId));
        chains.remove(routeId);
        log.info("Route '{}' removed", routeId);
    }

    public synchronized void setEnabled(String routeId, boolean enabled) {
        // Look up in allRoutes so disabled routes can also be toggled
        RouteDefinition route = allRoutes.get(routeId);
        if (route == null) {
            log.warn("setEnabled called for unknown route '{}'", routeId);
            return;
        }
        route.setEnabled(enabled);
        if (enabled) {
            routes.removeIf(r -> r.getId().equals(routeId)); // remove stale copy if any
            routes.add(route);
            chains.put(routeId, buildChain(route));
            log.info("Route '{}' enabled", routeId);
        } else {
            routes.removeIf(r -> r.getId().equals(routeId));
            chains.remove(routeId);
            log.info("Route '{}' disabled", routeId);
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean matches(RouteDefinition route, Context ctx) {
        return switch (route.getRoutingType()) {
            case PATH          -> pathMatcher.matches(route, ctx);
            case REGEX         -> regexMatcher.matches(route, ctx);
            case HEADER        -> headerMatcher.matches(route, ctx);
            case TRAFFIC_SPLIT -> trafficSplitMatcher.matches(route, ctx);
        };
    }

    private FilterChain buildChain(RouteDefinition route) {
        List<GatewayFilter> filters = new ArrayList<>();

        // 1. Tracing (global)
        filters.add(tracingFilter);

        // 2. Rate limiting (per-route, optional)
        if (route.getRateLimitPolicy() != null && route.getRateLimitPolicy().isEnabled()) {
            filters.add(new SlidingWindowRateLimiter(route));
        }

        // 3. Circuit breaker (per-route, optional)
        if (route.getCircuitBreakerPolicy() != null && route.getCircuitBreakerPolicy().isEnabled()) {
            filters.add(new CircuitBreakerGatewayFilter(
                    CircuitBreakerFactory.create(route), route.getId()));
        }

        // 4. Cache (per-route, optional, GET only)
        if (route.getCachePolicy() != null && route.getCachePolicy().isEnabled()) {
            filters.add(new CacheGatewayFilter(
                    new CaffeineCache(route.getCachePolicy().getTtlSeconds()),
                    route.getCachePolicy()));
        }

        // 5. X-Forwarded-For (global)
        filters.add(forwardedForFilter);

        // 6. Header mutation (per-route)
        if (route.getHeaderRules() != null) {
            filters.add(new HeaderMutationFilter(route.getHeaderRules()));
        }

        // 7. Body transform (per-route, pass-through by default)
        filters.add(new TransformGatewayFilter(null, null));

        // 8. Deduplicate response headers (per-route, optional)
        List<String> dedupe = route.getHeaderRules() != null
                ? route.getHeaderRules().getDedupeResponseHeaders()
                : List.of();
        if (!dedupe.isEmpty()) {
            filters.add(new DedupeResponseHeadersFilter(dedupe));
        }

        // 9. Proxy (terminal)
        filters.add(new ProxyFilter(createLoadBalancer(route), upstreamClient));

        return new DefaultFilterChain(filters);
    }

    private LoadBalancer createLoadBalancer(RouteDefinition route) {
        return switch (route.getLoadBalancerType()) {
            case WEIGHTED   -> new WeightedLoadBalancer();
            case RANDOM     -> new RandomLoadBalancer();
            default         -> new RoundRobinLoadBalancer();
        };
    }
}

