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
import org.example.utilities.gateway.model.HttpValidationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * GatewayFilter that blacklist-validates inbound request data using compiled
 * {@link Pattern}s loaded from the database via
 * {@link HttpValidationRuleProvider}.
 *
 * <p>The provider reference is a simple {@link Supplier} so this filter stays
 * in the {@code utilities} module (no direct dependency on JDBI or the
 * {@code app} layer).  The live {@link org.example.gateway.security.ValidationRuleStore}
 * is injected when the filter is registered in {@code RouteRegistry}.
 *
 * <p>Validation targets (controlled per-route via {@link HttpValidationPolicy}):
 * <ul>
 *   <li>Query parameters</li>
 *   <li>Request headers</li>
 *   <li>Cookie values</li>
 *   <li>Request body (disabled by default to avoid buffering large payloads)</li>
 * </ul>
 *
 * <p>On blacklist match: HTTP 400 with details of the first violation found.
 */
public class HttpValidationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpValidationFilter.class);

    /** Simple pair of compiled pattern + metadata. */
    public record ValidationRule(String id, String name, String target, Pattern pattern) {}

    /** Supplier interface that allows hot-reload without coupling to the app layer. */
    @FunctionalInterface
    public interface HttpValidationRuleProvider {
        List<ValidationRule> getRules();
    }

    private final HttpValidationPolicy policy;
    private final HttpValidationRuleProvider ruleProvider;

    public HttpValidationFilter(HttpValidationPolicy policy, HttpValidationRuleProvider ruleProvider) {
        this.policy       = policy;
        this.ruleProvider = ruleProvider;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        String path = ctx.getJavalinCtx().path();

        if (isExcluded(path)) {
            chain.proceed(ctx);
            return;
        }

        List<ValidationRule> rules = ruleProvider.getRules();
        Context jCtx = ctx.getJavalinCtx();

        // Validate query parameters
        if (policy.isValidateQueryParams()) {
            for (Map.Entry<String, List<String>> entry : jCtx.queryParamMap().entrySet()) {
                String paramName = entry.getKey();
                for (String value : entry.getValue()) {
                    String violation = findViolation(value, "QUERY_PARAM", rules);
                    if (violation != null) {
                        log.warn("HTTP validation failed path={} param={} rule={}",
                                path, paramName, violation);
                        ctx.getJavalinCtx().status(400)
                           .json(errorBody("Invalid query parameter '" + paramName + "': " + violation));
                        return;
                    }
                }
            }
        }

        // Validate headers
        if (policy.isValidateHeaders()) {
            for (Map.Entry<String, String> header : jCtx.headerMap().entrySet()) {
                // Skip common safe headers to reduce false positives
                String name = header.getKey().toLowerCase();
                if (name.equals("authorization") || name.equals("cookie")) continue;
                String violation = findViolation(header.getValue(), "HEADER", rules);
                if (violation != null) {
                    log.warn("HTTP validation failed path={} header={} rule={}",
                            path, header.getKey(), violation);
                    ctx.getJavalinCtx().status(400)
                       .json(errorBody("Invalid header '" + header.getKey() + "': " + violation));
                    return;
                }
            }
        }

        // Validate cookies
        if (policy.isValidateCookies()) {
            for (Map.Entry<String, String> cookie : jCtx.cookieMap().entrySet()) {
                String violation = findViolation(cookie.getValue(), "COOKIE", rules);
                if (violation != null) {
                    log.warn("HTTP validation failed path={} cookie={} rule={}",
                            path, cookie.getKey(), violation);
                    ctx.getJavalinCtx().status(400)
                       .json(errorBody("Invalid cookie '" + cookie.getKey() + "': " + violation));
                    return;
                }
            }
        }

        // Validate body (optional — disabled by default)
        if (policy.isValidateBody()) {
            byte[] bodyBytes = ctx.getEffectiveRequestBody();
            if (bodyBytes != null && bodyBytes.length > 0) {
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                String violation = findViolation(body, "BODY", rules);
                if (violation != null) {
                    log.warn("HTTP validation failed path={} target=BODY rule={}", path, violation);
                    ctx.getJavalinCtx().status(400)
                       .json(errorBody("Invalid request body: " + violation));
                    return;
                }
            }
        }

        chain.proceed(ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the name of the first rule that matches {@code value} for the
     * given target, or {@code null} if no rule matched.
     */
    private String findViolation(String value, String target, List<ValidationRule> rules) {
        if (value == null || value.isBlank()) return null;
        for (ValidationRule rule : rules) {
            if (appliesToTarget(rule.target(), target)) {
                if (rule.pattern().matcher(value).find()) {
                    return rule.name();
                }
            }
        }
        return null;
    }

    private boolean appliesToTarget(String ruleTarget, String requestTarget) {
        return "ALL".equalsIgnoreCase(ruleTarget) || ruleTarget.equalsIgnoreCase(requestTarget);
    }

    private boolean isExcluded(String path) {
        for (String prefix : policy.getExcludePaths()) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private static Map<String, Object> errorBody(String message) {
        return Map.of("status", 400, "error", message);
    }
}

