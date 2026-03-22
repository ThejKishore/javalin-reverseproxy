/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.filter;

import io.javalin.http.Context;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared per-request state passed through the {@link GatewayFilter} pipeline.
 *
 * <p>Filters read the incoming {@link Context}, populate upstream response fields,
 * and optionally override request/response bodies. After all filters complete,
 * {@link #applyToJavalinContext()} writes everything back to Javalin.
 */
public class FilterContext {

    // ── Immutable request-time data ──────────────────────────────────────────
    private final Context javalinCtx;
    private final RouteDefinition routeDefinition;
    private final long startTimeMs;

    // ── Set by TracingFilter ─────────────────────────────────────────────────
    private String requestId;
    private String traceId;

    // ── Set by ProxyFilter before the upstream call ──────────────────────────
    private TargetDefinition selectedTarget;

    // ── Populated by OkHttpUpstreamClient after the upstream call ────────────
    private int upstreamStatusCode = 200;
    /** Multi-value response headers from the upstream (excluding hop-by-hop). */
    private final Map<String, List<String>> upstreamResponseHeaders = new LinkedHashMap<>();
    private byte[] upstreamResponseBody = new byte[0];

    // ── Optional body overrides set by transform filters ─────────────────────
    private byte[] modifiedRequestBody;
    private byte[] modifiedResponseBody;

    // ── Built upstream URL for audit logging ─────────────────────────────────
    private String resolvedUpstreamUrl;

    public FilterContext(Context javalinCtx, RouteDefinition routeDefinition) {
        this.javalinCtx = javalinCtx;
        this.routeDefinition = routeDefinition;
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * Writes status code, headers, and body back to the Javalin {@link Context}.
     * Call this once, after the full filter chain completes.
     */
    public void applyToJavalinContext() {
        javalinCtx.status(upstreamStatusCode);
        upstreamResponseHeaders.forEach((name, values) ->
                values.forEach(value -> javalinCtx.header(name, value)));
        byte[] body = modifiedResponseBody != null ? modifiedResponseBody : upstreamResponseBody;
        if (body.length > 0) {
            javalinCtx.result(new ByteArrayInputStream(body));
        }
    }

    // ── Request body helpers ─────────────────────────────────────────────────

    /** Returns the (optionally modified) request body bytes. */
    public byte[] getEffectiveRequestBody() {
        if (modifiedRequestBody != null) return modifiedRequestBody;
        return javalinCtx.bodyAsBytes();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Context getJavalinCtx() { return javalinCtx; }
    public RouteDefinition getRouteDefinition() { return routeDefinition; }
    public long getStartTimeMs() { return startTimeMs; }
    public long elapsedMs() { return System.currentTimeMillis() - startTimeMs; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public TargetDefinition getSelectedTarget() { return selectedTarget; }
    public void setSelectedTarget(TargetDefinition selectedTarget) { this.selectedTarget = selectedTarget; }

    public int getUpstreamStatusCode() { return upstreamStatusCode; }
    public void setUpstreamStatusCode(int upstreamStatusCode) { this.upstreamStatusCode = upstreamStatusCode; }

    public Map<String, List<String>> getUpstreamResponseHeaders() { return upstreamResponseHeaders; }

    public void addUpstreamResponseHeader(String name, List<String> values) {
        upstreamResponseHeaders.put(name, new ArrayList<>(values));
    }

    public byte[] getUpstreamResponseBody() { return upstreamResponseBody; }
    public void setUpstreamResponseBody(byte[] upstreamResponseBody) {
        this.upstreamResponseBody = upstreamResponseBody != null ? upstreamResponseBody : new byte[0];
    }

    public byte[] getModifiedRequestBody() { return modifiedRequestBody; }
    public void setModifiedRequestBody(byte[] modifiedRequestBody) { this.modifiedRequestBody = modifiedRequestBody; }

    public byte[] getModifiedResponseBody() { return modifiedResponseBody; }
    public void setModifiedResponseBody(byte[] modifiedResponseBody) { this.modifiedResponseBody = modifiedResponseBody; }

    public String getResolvedUpstreamUrl() { return resolvedUpstreamUrl; }
    public void setResolvedUpstreamUrl(String resolvedUpstreamUrl) { this.resolvedUpstreamUrl = resolvedUpstreamUrl; }
}

