/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.header;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;

import java.util.List;
import java.util.Map;

/**
 * Collapses duplicate values in the nominated response headers into a single
 * comma-separated value.  Typical use-case: de-duplicating
 * {@code Access-Control-Allow-Origin} added by both the gateway and the upstream.
 */
public class DedupeResponseHeadersFilter implements GatewayFilter {

    private final List<String> headerNames;

    public DedupeResponseHeadersFilter(List<String> headerNames) {
        this.headerNames = headerNames;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        chain.proceed(ctx);

        Map<String, List<String>> headers = ctx.getUpstreamResponseHeaders();
        for (String name : headerNames) {
            List<String> values = headers.get(name);
            if (values == null || values.size() <= 1) continue;
            // Dedupe: keep unique values, preserve first-seen order
            List<String> deduped = values.stream().distinct().toList();
            headers.put(name, List.of(String.join(", ", deduped)));
        }
    }
}

