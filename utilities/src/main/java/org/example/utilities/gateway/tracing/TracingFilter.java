/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.tracing;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;

import java.util.List;
import java.util.UUID;

/**
 * Propagates or generates distributed-tracing identifiers.
 *
 * <ul>
 *   <li>{@code X-Request-ID} — unique per request; forwarded to upstream and echoed in response.</li>
 *   <li>{@code X-Trace-ID}   — propagated from upstream callers when present; otherwise generated.</li>
 * </ul>
 */
public class TracingFilter implements GatewayFilter {

    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String X_TRACE_ID = "X-Trace-ID";

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        String requestId = ctx.getJavalinCtx().header(X_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String traceId = ctx.getJavalinCtx().header(X_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ctx.setRequestId(requestId);
        ctx.setTraceId(traceId);

        chain.proceed(ctx);

        // Echo tracing headers back to the caller
        ctx.getUpstreamResponseHeaders().put(X_REQUEST_ID, List.of(requestId));
        ctx.getUpstreamResponseHeaders().put(X_TRACE_ID, List.of(traceId));
    }
}

