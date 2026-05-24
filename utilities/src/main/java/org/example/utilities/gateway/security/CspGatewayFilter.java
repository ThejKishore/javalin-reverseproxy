/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.security;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.CspPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GatewayFilter that injects a {@code Content-Security-Policy} (or
 * {@code Content-Security-Policy-Report-Only}) response header on each
 * proxied response.
 *
 * <p>This filter runs <em>after</em> the terminal {@code ProxyFilter} returns,
 * so it has access to the upstream response status.  Paths matching
 * {@code excludePaths} will not receive CSP headers.
 */
public class CspGatewayFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(CspGatewayFilter.class);

    private static final String CSP_HEADER         = "Content-Security-Policy";
    private static final String CSP_REPORT_ONLY    = "Content-Security-Policy-Report-Only";

    private final CspPolicy policy;

    public CspGatewayFilter(CspPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        String path = ctx.getJavalinCtx().path();

        if (isExcluded(path)) {
            chain.proceed(ctx);
            return;
        }

        // Let the rest of the chain (including the upstream call) complete first
        chain.proceed(ctx);

        // Add CSP header to response
        String headerName = policy.isReportOnly() ? CSP_REPORT_ONLY : CSP_HEADER;
        ctx.getJavalinCtx().header(headerName, policy.getPolicy());
        log.debug("CSP header injected path={} reportOnly={}", path, policy.isReportOnly());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExcluded(String path) {
        for (String prefix : policy.getExcludePaths()) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }
}

