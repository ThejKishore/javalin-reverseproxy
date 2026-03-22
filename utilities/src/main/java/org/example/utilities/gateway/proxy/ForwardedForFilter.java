/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.proxy;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;

import java.util.List;

/**
 * Appends the client's IP address to the {@code X-Forwarded-For} header before
 * forwarding the request to the upstream server.
 */
public class ForwardedForFilter implements GatewayFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        String clientIp = ctx.getJavalinCtx().ip();
        // Append to any existing X-Forwarded-For chain
        String existing = ctx.getJavalinCtx().header(X_FORWARDED_FOR);
        String forwardedFor = (existing != null && !existing.isBlank())
                ? existing + ", " + clientIp
                : clientIp;
        // Stash in context so OkHttpUpstreamClient adds them to the outgoing request
        ctx.getUpstreamResponseHeaders(); // ensure map is initialised (no-op)
        // We use Javalin context attributes as a side-channel for extra request headers
        ctx.getJavalinCtx().attribute(X_FORWARDED_FOR, forwardedFor);
        ctx.getJavalinCtx().attribute(X_REAL_IP, clientIp);
        chain.proceed(ctx);
    }
}

