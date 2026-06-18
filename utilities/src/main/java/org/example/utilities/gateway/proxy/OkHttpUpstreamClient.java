/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.proxy;

import okhttp3.*;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.HeaderRules;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp-based {@link UpstreamHttpClient}.
 *
 * <ul>
 *   <li>Per-route {@link OkHttpClient} instances with custom timeouts are cached.</li>
 *   <li>Hop-by-hop headers are stripped before forwarding.</li>
 *   <li>Tracing headers ({@code X-Request-ID}, {@code X-Trace-ID}) and
 *       {@code X-Forwarded-For} are injected.</li>
 *   <li>Route-level {@code add-request} / {@code exclude-request} header rules are applied.</li>
 *   <li>HTTP/2 and HTTPS are supported transparently by OkHttp.</li>
 * </ul>
 */
public class OkHttpUpstreamClient implements UpstreamHttpClient {

    private static final Logger log = LoggerFactory.getLogger(OkHttpUpstreamClient.class);

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    /** One OkHttpClient per route (different timeouts). */
    private final ConcurrentHashMap<String, OkHttpClient> clientCache = new ConcurrentHashMap<>();

    @Override
    public void forward(FilterContext ctx) throws Exception {
        RouteDefinition route = ctx.getRouteDefinition();
        TargetDefinition target = ctx.getSelectedTarget();

        String upstreamUrl = buildUpstreamUrl(target.getUrl(), ctx);
        ctx.setResolvedUpstreamUrl(upstreamUrl);
        log.debug("Forwarding {} {} → {}", ctx.getJavalinCtx().method().name(),
                ctx.getJavalinCtx().path(), upstreamUrl);

        OkHttpClient client = clientCache.computeIfAbsent(route.getId(), id ->
                new OkHttpClient.Builder()
                        .connectTimeout(route.getTimeoutMs(), TimeUnit.MILLISECONDS)
                        .readTimeout(route.getTimeoutMs(), TimeUnit.MILLISECONDS)
                        .writeTimeout(route.getTimeoutMs(), TimeUnit.MILLISECONDS)
                        .followRedirects(true)
                        .build()
        );

        Request.Builder reqBuilder = new Request.Builder().url(upstreamUrl);

        // Copy client headers (skip hop-by-hop)
        ctx.getJavalinCtx().headerMap().forEach((name, value) -> {
            if (!HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                reqBuilder.header(name, value);
            }
        });

        // Apply route-level request header rules
        HeaderRules headerRules = route.getHeaderRules();
        if (headerRules != null) {
            if (headerRules.getAddRequest() != null) {
                headerRules.getAddRequest().forEach(e -> reqBuilder.header(e.getName(), e.getValue()));
            }
            if (headerRules.getExcludeRequest() != null) {
                headerRules.getExcludeRequest().forEach(reqBuilder::removeHeader);
            }
        }

        // Tracing headers
        if (ctx.getRequestId() != null) reqBuilder.header("X-Request-ID", ctx.getRequestId());
        if (ctx.getTraceId() != null)   reqBuilder.header("X-Trace-ID",   ctx.getTraceId());

        // Forwarded-For (set by ForwardedForFilter via Javalin attributes)
        String xForwardedFor = ctx.getJavalinCtx().attribute("X-Forwarded-For");
        if (xForwardedFor != null) reqBuilder.header("X-Forwarded-For", xForwardedFor);
        String xRealIp = ctx.getJavalinCtx().attribute("X-Real-IP");
        if (xRealIp != null) reqBuilder.header("X-Real-IP", xRealIp);

        // Auth-forward headers
        List<String> authHeaders = route.getAuthForwardHeaders();
        if (authHeaders != null) {
            authHeaders.forEach(h -> {
                String v = ctx.getJavalinCtx().header(h);
                if (v != null) reqBuilder.header(h, v);
            });
        }

        // Build request body
        String method = ctx.getJavalinCtx().method().name();
        RequestBody body = buildRequestBody(ctx);
        reqBuilder.method(method, body);

        try (Response resp = client.newCall(reqBuilder.build()).execute()) {
            ctx.setUpstreamStatusCode(resp.code());
            resp.headers().toMultimap().forEach((name, values) -> {
                if (!HOP_BY_HOP.contains(name.toLowerCase(Locale.ROOT))) {
                    ctx.addUpstreamResponseHeader(name, values);
                }
            });
            ctx.setUpstreamResponseBody(resp.body() != null ? resp.body().bytes() : new byte[0]);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildUpstreamUrl(String targetBase, FilterContext ctx) {
        String path = ctx.getJavalinCtx().path();
        String stripPrefix = ctx.getRouteDefinition().getStripPrefix();
        if (stripPrefix != null && !stripPrefix.isBlank() && path.startsWith(stripPrefix)) {
            path = path.substring(stripPrefix.length());
        }
        if (!path.startsWith("/")) path = "/" + path;
        if (targetBase.endsWith("/")) targetBase = targetBase.substring(0, targetBase.length() - 1);

        String url = targetBase + path;
        String qs = ctx.getJavalinCtx().queryString();
        if (qs != null && !qs.isEmpty()) url += "?" + qs;
        return url;
    }

    private RequestBody buildRequestBody(FilterContext ctx) {
        String method = ctx.getJavalinCtx().method().name();
        if (Set.of("GET", "HEAD", "DELETE", "OPTIONS", "TRACE").contains(method)) return null;
        byte[] body = ctx.getEffectiveRequestBody();
        String contentType = ctx.getJavalinCtx().contentType();
        MediaType mediaType = (contentType != null) ? MediaType.parse(contentType) : null;
        return RequestBody.create(body != null ? body : new byte[0], mediaType);
    }
}

