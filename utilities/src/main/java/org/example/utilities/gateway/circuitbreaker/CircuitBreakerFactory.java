/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.utilities.gateway.model.CircuitBreakerPolicy;
import org.example.utilities.gateway.model.RouteDefinition;

import java.time.Duration;

/** Builds and caches Resilience4j {@link CircuitBreaker} instances per route. */
public class CircuitBreakerFactory {

    public static CircuitBreaker create(RouteDefinition route) {
        CircuitBreakerPolicy policy = route.getCircuitBreakerPolicy();
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(policy.getSlidingWindowSize())
                        .failureRateThreshold(policy.getFailureRateThreshold())
                        .waitDurationInOpenState(Duration.ofSeconds(policy.getWaitDurationSeconds()))
                        .permittedNumberOfCallsInHalfOpenState(Math.max(1, policy.getSlidingWindowSize() / 2))
                        .build();
        return CircuitBreakerRegistry.of(config).circuitBreaker(route.getId());
    }
}

