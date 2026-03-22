/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.cache;

import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.CachePolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Short-circuits the pipeline with a cached response when available.
 * On a cache miss the upstream call proceeds normally and a 2xx response
 * is stored for the configured TTL.
 */
public class CacheGatewayFilter implements GatewayFilter {

    private final CacheStore store;
    private final CachePolicy policy;

    public CacheGatewayFilter(CacheStore store, CachePolicy policy) {
        this.store = store;
        this.policy = policy;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        if (!"GET".equalsIgnoreCase(ctx.getJavalinCtx().method().name())) {
            chain.proceed(ctx);
            return;
        }
        String key = buildKey(ctx);
        Optional<CacheStore.CachedResponse> cached = store.get(key);
        if (cached.isPresent()) {
            CacheStore.CachedResponse r = cached.get();
            ctx.setUpstreamStatusCode(r.statusCode());
            r.headers().forEach(ctx::addUpstreamResponseHeader);
            ctx.setUpstreamResponseBody(r.body());
            ctx.getJavalinCtx().header("X-Cache", "HIT");
            return; // skip chain — serve from cache
        }
        chain.proceed(ctx);
        // Cache on 2xx
        if (ctx.getUpstreamStatusCode() >= 200 && ctx.getUpstreamStatusCode() < 300) {
            var headers = new LinkedHashMap<String, List<String>>(ctx.getUpstreamResponseHeaders());
            store.put(key, new CacheStore.CachedResponse(
                    ctx.getUpstreamStatusCode(), headers, ctx.getUpstreamResponseBody()));
            ctx.getJavalinCtx().header("X-Cache", "MISS");
        }
    }

    private String buildKey(FilterContext ctx) {
        String path = ctx.getJavalinCtx().path();
        if ("METHOD_PATH_QUERY".equals(policy.getCacheKeyStrategy())) {
            String qs = ctx.getJavalinCtx().queryString();
            return ctx.getJavalinCtx().method().name() + ":" + path
                    + (qs != null && !qs.isEmpty() ? "?" + qs : "");
        }
        return ctx.getJavalinCtx().method().name() + ":" + path;
    }
}

