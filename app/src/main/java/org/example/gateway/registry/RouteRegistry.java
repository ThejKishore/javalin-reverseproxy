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
import org.example.utilities.gateway.model.CspPolicy;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.example.utilities.gateway.model.JwtPolicy;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.proxy.ForwardedForFilter;
import org.example.utilities.gateway.proxy.OkHttpUpstreamClient;
import org.example.utilities.gateway.proxy.ProxyFilter;
import org.example.utilities.gateway.ratelimit.SlidingWindowRateLimiter;
import org.example.utilities.gateway.routing.HeaderRouteMatcher;
import org.example.utilities.gateway.routing.PathRouteMatcher;
import org.example.utilities.gateway.routing.RegexRouteMatcher;
import org.example.utilities.gateway.routing.TrafficSplitRouteMatcher;
import org.example.utilities.gateway.security.*;
import org.example.utilities.gateway.tracing.TracingFilter;
import org.example.utilities.gateway.transform.TransformGatewayFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    /** Optional CSRF token store — null if Hazelcast is disabled. */
    private CsrfTokenStore csrfTokenStore;

    /** Optional validation rule provider — null if DB is unavailable. */
    private HttpValidationFilter.HttpValidationRuleProvider validationRuleProvider;

    /**
     * Global JWT policy applied to all routes unless overridden per-route.
     * When null, only routes with an explicit {@code jwt-policy} enforce JWT.
     */
    private JwtPolicy globalJwtPolicy;

    /** Global CSRF policy — fallback when a route has no explicit csrf-policy. */
    private CsrfPolicy globalCsrfPolicy;

    /** Global CSP policy — fallback when a route has no explicit csp-policy. */
    private CspPolicy globalCspPolicy;

    public RouteRegistry() {}

    /**
     * Injects the distributed CSRF token store (from Hazelcast).
     * Must be called before {@link #reload(List)}.
     */
    public void setCsrfTokenStore(CsrfTokenStore csrfTokenStore) {
        this.csrfTokenStore = csrfTokenStore;
    }

    /**
     * Injects the validation rule provider (from {@code ValidationRuleStore}).
     * Must be called before {@link #reload(List)}.
     */
    public void setValidationRuleProvider(HttpValidationFilter.HttpValidationRuleProvider validationRuleProvider) {
        this.validationRuleProvider = validationRuleProvider;
    }

    /**
     * Sets the global JWT policy applied to all routes unless they explicitly
     * override it.  Safe to call at any time — existing chains are rebuilt
     * immediately so the new policy takes effect without a full reload.
     */
    public synchronized void setGlobalJwtPolicy(JwtPolicy globalJwtPolicy) {
        this.globalJwtPolicy = globalJwtPolicy;
        // Rebuild all active chains so the new policy is picked up immediately
        for (RouteDefinition r : routes) {
            chains.put(r.getId(), buildChain(r));
        }
    }

    /**
     * Sets the global CSRF policy.  Rebuilds all active chains immediately.
     */
    public synchronized void setGlobalCsrfPolicy(CsrfPolicy globalCsrfPolicy) {
        this.globalCsrfPolicy = globalCsrfPolicy;
        for (RouteDefinition r : routes) {
            chains.put(r.getId(), buildChain(r));
        }
    }

    /**
     * Sets the global CSP policy.  Rebuilds all active chains immediately.
     */
    public synchronized void setGlobalCspPolicy(CspPolicy globalCspPolicy) {
        this.globalCspPolicy = globalCspPolicy;
        for (RouteDefinition r : routes) {
            chains.put(r.getId(), buildChain(r));
        }
    }

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

        // 1. JWT authentication — route-level policy takes precedence over global.
        //    If the route has no explicit policy, fall back to the global policy.
        //    A route can opt-out by setting jwt-policy.enabled=false.
        JwtPolicy effectiveJwt = resolveJwtPolicy(route);
        if (effectiveJwt != null && effectiveJwt.isEnabled()) {
            filters.add(new JwtAuthFilter(effectiveJwt));
            log.debug("Route '{}' JWT filter active (source: {})",
                    route.getId(),
                    route.getJwtPolicy() != null ? "route" : "global");
        }

        // 2. CSRF protection (per-route, optional) — unsafe methods only
        CsrfPolicy effectiveCsrf = resolveCsrfPolicy(route);
        if (effectiveCsrf != null && effectiveCsrf.isEnabled() && csrfTokenStore != null) {
            filters.add(new CsrfGatewayFilter(effectiveCsrf, csrfTokenStore));
        }

        // 3. Tracing (global)
        filters.add(tracingFilter);

        // 4. Rate limiting (per-route, optional)
        if (route.getRateLimitPolicy() != null && route.getRateLimitPolicy().isEnabled()) {
            filters.add(new SlidingWindowRateLimiter(route));
        }

        // 5. Circuit breaker (per-route, optional)
        if (route.getCircuitBreakerPolicy() != null && route.getCircuitBreakerPolicy().isEnabled()) {
            filters.add(new CircuitBreakerGatewayFilter(
                    CircuitBreakerFactory.create(route), route.getId()));
        }

        // 6. Cache (per-route, optional, GET only)
        if (route.getCachePolicy() != null && route.getCachePolicy().isEnabled()) {
            filters.add(new CacheGatewayFilter(
                    new CaffeineCache(route.getCachePolicy().getTtlSeconds()),
                    route.getCachePolicy()));
        }

        // 7. X-Forwarded-For (global)
        filters.add(forwardedForFilter);

        // 8. HTTP input validation (per-route, optional)
        if (route.getHttpValidationPolicy() != null && route.getHttpValidationPolicy().isEnabled()
                && validationRuleProvider != null) {
            filters.add(new HttpValidationFilter(route.getHttpValidationPolicy(), validationRuleProvider));
        }

        // 9. Header mutation (per-route)
        if (route.getHeaderRules() != null) {
            filters.add(new HeaderMutationFilter(route.getHeaderRules()));
        }

        // 10. Body transform (per-route, pass-through by default)
        filters.add(new TransformGatewayFilter(null, null));

        // 11. Deduplicate response headers (per-route, optional)
        List<String> dedupe = route.getHeaderRules() != null
                ? route.getHeaderRules().getDedupeResponseHeaders()
                : List.of();
        if (!dedupe.isEmpty()) {
            filters.add(new DedupeResponseHeadersFilter(dedupe));
        }

        // 12. CSP header injection — must be before terminal proxy
        CspPolicy effectiveCsp = resolveCspPolicy(route);
        if (effectiveCsp != null && effectiveCsp.isEnabled()) {
            filters.add(new CspGatewayFilter(effectiveCsp));
        }

        // 13. Proxy (terminal)
        filters.add(new ProxyFilter(createLoadBalancer(route), upstreamClient));

        return new DefaultFilterChain(filters);
    }

    /**
     * Resolves the effective JWT policy for a route.
     *
     * <ul>
     *   <li>If the route has an explicit {@code jwt-policy}, that wins for all
     *       settings (credentials, algorithm, required-claims).</li>
     *   <li>If the route has no explicit policy, the global policy is used.</li>
     *   <li>In both cases the global {@code exclude-paths} are <em>merged in</em>
     *       as a superset, so paths that should never be token-checked remain
     *       exempt regardless of per-route configuration.</li>
     * </ul>
     */
    private JwtPolicy resolveJwtPolicy(RouteDefinition route) {
        JwtPolicy routePolicy  = route.getJwtPolicy();
        JwtPolicy globalPolicy = this.globalJwtPolicy;

        // Determine which policy provides credentials/settings
        JwtPolicy base = (routePolicy != null) ? routePolicy : globalPolicy;
        if (base == null) return null;

        // If there is a global policy with exclude-paths, merge them into the
        // effective policy so they always apply even when route overrides jwt-policy.
        if (globalPolicy != null && !globalPolicy.getExcludePaths().isEmpty()
                && routePolicy != null) {
            // Build a merged copy: start from the route policy, add global excludes
            JwtPolicy merged = shallowCopy(routePolicy);
            java.util.List<String> combined = new java.util.ArrayList<>(routePolicy.getExcludePaths());
            for (String p : globalPolicy.getExcludePaths()) {
                if (!combined.contains(p)) combined.add(p);
            }
            merged.setExcludePaths(combined);
            return merged;
        }

        return base;
    }

    /** Shallow-copies a {@link JwtPolicy} so we can mutate exclude-paths safely. */
    private static JwtPolicy shallowCopy(JwtPolicy src) {
        JwtPolicy copy = new JwtPolicy();
        copy.setEnabled(src.isEnabled());
        copy.setAlgorithm(src.getAlgorithm());
        copy.setSecretOrPublicKey(src.getSecretOrPublicKey());
        copy.setIssuer(src.getIssuer());
        copy.setAudience(src.getAudience());
        copy.setRequiredClaims(new java.util.LinkedHashMap<>(src.getRequiredClaims()));
        copy.setExcludePaths(new java.util.ArrayList<>(src.getExcludePaths()));
        return copy;
    }

    /**
     * Resolves the effective CSRF policy for a route.
     * Per-route policy overrides global; global exclude-paths are merged in.
     */
    private CsrfPolicy resolveCsrfPolicy(RouteDefinition route) {
        CsrfPolicy routePolicy  = route.getCsrfPolicy();
        CsrfPolicy globalPolicy = this.globalCsrfPolicy;
        CsrfPolicy base = (routePolicy != null) ? routePolicy : globalPolicy;
        if (base == null) return null;

        if (globalPolicy != null && !globalPolicy.getExcludePaths().isEmpty() && routePolicy != null) {
            CsrfPolicy merged = new CsrfPolicy();
            merged.setEnabled(routePolicy.isEnabled());
            merged.setTokenTtlSeconds(routePolicy.getTokenTtlSeconds());
            merged.setBindTo(routePolicy.getBindTo());
            java.util.List<String> combined = new java.util.ArrayList<>(routePolicy.getExcludePaths());
            for (String p : globalPolicy.getExcludePaths()) {
                if (!combined.contains(p)) combined.add(p);
            }
            merged.setExcludePaths(combined);
            return merged;
        }
        return base;
    }

    /**
     * Resolves the effective CSP policy for a route.
     * Per-route policy overrides global; global exclude-paths are merged in.
     */
    private CspPolicy resolveCspPolicy(RouteDefinition route) {
        CspPolicy routePolicy  = route.getCspPolicy();
        CspPolicy globalPolicy = this.globalCspPolicy;
        CspPolicy base = (routePolicy != null) ? routePolicy : globalPolicy;
        if (base == null) return null;

        if (globalPolicy != null && !globalPolicy.getExcludePaths().isEmpty() && routePolicy != null) {
            CspPolicy merged = new CspPolicy();
            merged.setEnabled(routePolicy.isEnabled());
            merged.setPolicy(routePolicy.getPolicy());
            merged.setReportOnly(routePolicy.isReportOnly());
            java.util.List<String> combined = new java.util.ArrayList<>(routePolicy.getExcludePaths());
            for (String p : globalPolicy.getExcludePaths()) {
                if (!combined.contains(p)) combined.add(p);
            }
            merged.setExcludePaths(combined);
            return merged;
        }
        return base;
    }

    private LoadBalancer createLoadBalancer(RouteDefinition route) {
        return switch (route.getLoadBalancerType()) {
            case WEIGHTED -> new WeightedLoadBalancer();
            case RANDOM   -> new RandomLoadBalancer();
            case HEADER   -> new HeaderLoadBalancer();
            default       -> new RoundRobinLoadBalancer();
        };
    }
}

