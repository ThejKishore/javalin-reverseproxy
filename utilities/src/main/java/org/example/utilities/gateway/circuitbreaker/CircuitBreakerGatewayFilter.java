/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.example.utilities.gateway.exception.CircuitBreakerOpenException;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;

/**
 * Wraps the downstream filter chain in a Resilience4j {@link CircuitBreaker}.
 * If the upstream call fails repeatedly the circuit opens and immediately throws
 * {@link CircuitBreakerOpenException} (HTTP 503) until the wait duration passes.
 */
public class CircuitBreakerGatewayFilter implements GatewayFilter {

    private final CircuitBreaker circuitBreaker;
    private final String routeId;

    public CircuitBreakerGatewayFilter(CircuitBreaker circuitBreaker, String routeId) {
        this.circuitBreaker = circuitBreaker;
        this.routeId = routeId;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        try {
            circuitBreaker.acquirePermission();
            long start = System.nanoTime();
            try {
                chain.proceed(ctx);
                circuitBreaker.onSuccess(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (Exception ex) {
                circuitBreaker.onError(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS, ex);
                throw ex;
            }
        } catch (CallNotPermittedException e) {
            throw new CircuitBreakerOpenException(routeId);
        }
    }
}

