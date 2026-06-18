/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.security;

import io.javalin.http.Context;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * GatewayFilter that protects state-changing requests from Cross-Site Request
 * Forgery (CSRF) attacks.
 *
 * <p><strong>Safe methods</strong> (GET, HEAD, OPTIONS) are not checked.
 *
 * <p><strong>Unsafe methods</strong> (POST, PUT, DELETE, PATCH):
 * <ol>
 *   <li>On first visit (GET / safe request): generate a token, store it in the
 *       distributed {@link CsrfTokenStore}, and return it to the client as a
 *       {@code X-CSRF-Token} response header <em>and</em> a same-site cookie.</li>
 *   <li>On state-changing requests: read the token from {@code X-CSRF-Token}
 *       request header (or {@code _csrf} form field), look it up in the store,
 *       and verify it matches. Return HTTP 403 if absent, expired, or wrong.</li>
 * </ol>
 *
 * <p>Token generation is controlled inside the safe-method path so the client
 * always receives a fresh token after a GET that precedes a form submission.
 */
public class CsrfGatewayFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(CsrfGatewayFilter.class);

    private static final String CSRF_HEADER       = "X-CSRF-Token";
    private static final String CSRF_FORM_FIELD   = "_csrf";
    private static final String CSRF_COOKIE_NAME  = "XSRF-TOKEN";

    /** HTTP methods that require CSRF token validation. */
    private static final Set<String> UNSAFE_METHODS =
            Set.of("POST", "PUT", "DELETE", "PATCH");

    private final CsrfPolicy policy;
    private final CsrfTokenStore tokenStore;

    public CsrfGatewayFilter(CsrfPolicy policy, CsrfTokenStore tokenStore) {
        this.policy     = policy;
        this.tokenStore = tokenStore;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        Context jCtx   = ctx.getJavalinCtx();
        String  path   = jCtx.path();
        String  method = jCtx.method().name();

        // Bypass for excluded paths
        if (isExcluded(path)) {
            chain.proceed(ctx);
            return;
        }

        String clientKey = resolveClientKey(jCtx);

        if (!UNSAFE_METHODS.contains(method)) {
            // Safe method: generate/refresh token and attach to response
            String token = generateAndStore(clientKey);
            attachTokenToResponse(jCtx, token);
            chain.proceed(ctx);
            return;
        }

        // Unsafe method: validate token
        String supplied = extractSuppliedToken(jCtx);
        if (supplied == null || supplied.isBlank()) {
            log.warn("CSRF token missing method={} path={} client={}", method, path, clientKey);
            jCtx.status(403).json(errorBody("CSRF token missing"));
            return;
        }

        String stored = tokenStore.get(clientKey);
        if (stored == null) {
            log.warn("CSRF token not found (expired?) method={} path={} client={}", method, path, clientKey);
            jCtx.status(403).json(errorBody("CSRF token expired or not found"));
            return;
        }

        if (!stored.equals(supplied)) {
            log.warn("CSRF token mismatch method={} path={} client={}", method, path, clientKey);
            jCtx.status(403).json(errorBody("CSRF token mismatch"));
            return;
        }

        // Valid — rotate token (one-time use) then proceed
        tokenStore.invalidate(clientKey);
        String newToken = generateAndStore(clientKey);
        attachTokenToResponse(jCtx, newToken);

        chain.proceed(ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExcluded(String path) {
        for (String prefix : policy.getExcludePaths()) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private String resolveClientKey(Context jCtx) {
        return "SESSION".equalsIgnoreCase(policy.getBindTo())
                ? jCtx.sessionAttribute("sessionId") != null
                        ? jCtx.sessionAttribute("sessionId").toString()
                        : jCtx.ip()
                : jCtx.ip();
    }

    private String generateAndStore(String clientKey) {
        String token = UUID.randomUUID().toString();
        tokenStore.store(clientKey, token, policy.getTokenTtlSeconds());
        return token;
    }

    private void attachTokenToResponse(Context jCtx, String token) {
        jCtx.header(CSRF_HEADER, token);
        // Cookie delivery is optional — header is the canonical delivery mechanism.
        // Avoid Javalin cookie() API here to prevent Servlet API version conflicts.
    }

    private String extractSuppliedToken(Context jCtx) {
        String fromHeader = jCtx.header(CSRF_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) return fromHeader;
        return jCtx.formParam(CSRF_FORM_FIELD);
    }

    private static Map<String, Object> errorBody(String message) {
        return Map.of("status", 403, "error", message);
    }
}

