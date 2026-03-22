/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.filter;

/**
 * A single unit of gateway processing logic.
 *
 * <p>Implementations must either call {@code chain.proceed(ctx)} to continue the
 * pipeline or short-circuit (e.g. write a cached response) without calling proceed.
 * Code placed <em>before</em> {@code chain.proceed()} runs on the way <em>in</em>;
 * code placed <em>after</em> runs on the way <em>out</em> (post-upstream).
 */
@FunctionalInterface
public interface GatewayFilter {
    void filter(FilterContext ctx, FilterChain chain) throws Exception;
}

