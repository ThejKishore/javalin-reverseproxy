/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.proxy;

import org.example.utilities.gateway.exception.NoTargetAvailableException;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.loadbalancer.LoadBalancer;
import org.example.utilities.gateway.model.TargetDefinition;

import java.util.List;

/**
 * Terminal filter — selects a target via the route's load balancer then
 * delegates to {@link UpstreamHttpClient} to call the upstream server.
 * Does <em>not</em> call {@code chain.proceed()}.
 */
public class ProxyFilter implements GatewayFilter {

    private final LoadBalancer loadBalancer;
    private final UpstreamHttpClient upstreamClient;

    public ProxyFilter(LoadBalancer loadBalancer, UpstreamHttpClient upstreamClient) {
        this.loadBalancer = loadBalancer;
        this.upstreamClient = upstreamClient;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        List<TargetDefinition> targets = ctx.getRouteDefinition().getTargets();
        TargetDefinition target = loadBalancer.pick(targets, ctx);   // context-aware
        if (target == null) {
            throw new NoTargetAvailableException(ctx.getRouteDefinition().getId());
        }
        ctx.setSelectedTarget(target);
        upstreamClient.forward(ctx);
        // Terminal — intentionally no chain.proceed()
    }
}

