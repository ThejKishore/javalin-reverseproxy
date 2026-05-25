/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.loadbalancer;

import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.TargetDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Routes the request to the first target whose {@code header-match-value}
 * matches the value of the request header specified by {@code header-match-name}.
 *
 * <p>Example YAML configuration:
 * <pre>{@code
 * load-balancer-type: HEADER
 * targets:
 *   - url: http://beta-group:8080
 *     header-match-name: X-User-Group
 *     header-match-value: "beta"
 *   - url: http://default-group:8080
 *     header-match-name: X-User-Group
 *     header-match-value: "default"
 * }</pre>
 *
 * <p>If no target matches the header value the first target in the list is
 * used as a fallback so the request is never silently dropped.
 */
public class HeaderLoadBalancer implements LoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(HeaderLoadBalancer.class);

    /**
     * Context-unaware fallback — never used in normal operation because
     * {@link ProxyFilter} always calls {@link #pick(List, FilterContext)}.
     * Falls back to the first target.
     */
    @Override
    public TargetDefinition pick(List<TargetDefinition> targets) {
        if (targets == null || targets.isEmpty()) return null;
        return targets.get(0);
    }

    /**
     * Reads the request header from context and routes to the matching target.
     */
    @Override
    public TargetDefinition pick(List<TargetDefinition> targets, FilterContext ctx) {
        if (targets == null || targets.isEmpty()) return null;
        if (ctx == null) return targets.get(0);

        for (TargetDefinition target : targets) {
            String headerName  = target.getHeaderMatchName();
            String headerValue = target.getHeaderMatchValue();

            if (headerName == null || headerName.isBlank()) continue;

            String requestValue = ctx.getJavalinCtx().header(headerName);
            if (headerValue != null && headerValue.equals(requestValue)) {
                log.debug("HeaderLoadBalancer: header {}={} → {}", headerName, requestValue, target.getUrl());
                return target;
            }
        }

        // No explicit match — use first target as default
        log.debug("HeaderLoadBalancer: no matching target found, using default ({})", targets.get(0).getUrl());
        return targets.get(0);
    }
}

