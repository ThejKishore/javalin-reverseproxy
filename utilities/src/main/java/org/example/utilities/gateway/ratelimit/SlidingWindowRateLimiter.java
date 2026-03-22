/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.example.utilities.gateway.exception.RateLimitExceededException;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.RateLimitPolicy;
import org.example.utilities.gateway.model.RouteDefinition;

import java.time.Duration;

/**
 * Enforces per-route rate limits using Resilience4j's sliding-window {@link RateLimiter}.
 * A single instance of this filter is created per route by the {@code RouteRegistry}.
 */
public class SlidingWindowRateLimiter implements GatewayFilter {

    private final RateLimiter rateLimiter;
    private final String routeId;

    public SlidingWindowRateLimiter(RouteDefinition route) {
        this.routeId = route.getId();
        RateLimitPolicy policy = route.getRateLimitPolicy();
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(policy.getBurst() > 0 ? policy.getBurst() : policy.getRequestsPerSecond())
                .timeoutDuration(Duration.ofMillis(policy.getTimeoutDurationMs()))
                .build();
        this.rateLimiter = RateLimiterRegistry.of(config).rateLimiter(routeId);
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        if (!rateLimiter.acquirePermission()) {
            throw new RateLimitExceededException(routeId);
        }
        chain.proceed(ctx);
    }
}

