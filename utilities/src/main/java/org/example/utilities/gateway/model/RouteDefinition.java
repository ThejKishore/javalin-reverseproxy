/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete definition of a single gateway route.
 * Instances are loaded from {@code application.yml} or a database and live in the
 * {@code RouteRegistry}. Jackson's KEBAB_CASE property-naming strategy maps
 * YAML keys (e.g. {@code path-pattern}) to the corresponding Java fields.
 */
public class RouteDefinition {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    /** URL path pattern, e.g. {@code /api/users} or {@code /api/.*} for regex. */
    @JsonProperty("path-pattern")
    private String pathPattern;

    @JsonProperty("routing-type")
    private RoutingType routingType = RoutingType.PATH;

    /**
     * Prefix to strip from the request path before forwarding.
     * E.g., if pattern is {@code /api/v1} and strip-prefix is {@code /api/v1},
     * a request to {@code /api/v1/users} is forwarded as {@code /users}.
     */
    @JsonProperty("strip-prefix")
    private String stripPrefix;

    @JsonProperty("enabled")
    private boolean enabled = true;

    /** Per-route upstream connect/read/write timeout in milliseconds. */
    @JsonProperty("timeout-ms")
    private int timeoutMs = 5000;

    @JsonProperty("load-balancer-type")
    private LoadBalancerType loadBalancerType = LoadBalancerType.ROUND_ROBIN;

    @JsonProperty("targets")
    private List<TargetDefinition> targets = new ArrayList<>();

    @JsonProperty("rate-limit-policy")
    private RateLimitPolicy rateLimitPolicy;

    @JsonProperty("circuit-breaker-policy")
    private CircuitBreakerPolicy circuitBreakerPolicy;

    @JsonProperty("cache-policy")
    private CachePolicy cachePolicy;

    @JsonProperty("header-rules")
    private HeaderRules headerRules = new HeaderRules();

    /** Headers whose values should be forwarded to the upstream (e.g. Authorization). */
    @JsonProperty("auth-forward-headers")
    private List<String> authForwardHeaders = new ArrayList<>();

    @JsonProperty("audit-enabled")
    private boolean auditEnabled = false;

    /** Where to store audit records: {@code database} or {@code file}. */
    @JsonProperty("audit-store")
    private String auditStore = "database";

    /** Header name used for HEADER routing type. */
    @JsonProperty("header-match-name")
    private String headerMatchName;

    /** Expected header value used for HEADER routing type. */
    @JsonProperty("header-match-value")
    private String headerMatchValue;

    /** JWT authentication policy (optional, per-route). */
    @JsonProperty("jwt-policy")
    private JwtPolicy jwtPolicy;

    /** CSRF protection policy (optional, per-route). */
    @JsonProperty("csrf-policy")
    private CsrfPolicy csrfPolicy;

    /** Content Security Policy (optional, per-route). */
    @JsonProperty("csp-policy")
    private CspPolicy cspPolicy;

    /** HTTP input validation policy using OWASP ESAPI (optional, per-route). */
    @JsonProperty("http-validation-policy")
    private HttpValidationPolicy httpValidationPolicy;

    // --- Getters & setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }

    public RoutingType getRoutingType() { return routingType; }
    public void setRoutingType(RoutingType routingType) { this.routingType = routingType; }

    public String getStripPrefix() { return stripPrefix; }
    public void setStripPrefix(String stripPrefix) { this.stripPrefix = stripPrefix; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public LoadBalancerType getLoadBalancerType() { return loadBalancerType; }
    public void setLoadBalancerType(LoadBalancerType loadBalancerType) { this.loadBalancerType = loadBalancerType; }

    public List<TargetDefinition> getTargets() { return targets; }
    public void setTargets(List<TargetDefinition> targets) { this.targets = targets; }

    public RateLimitPolicy getRateLimitPolicy() { return rateLimitPolicy; }
    public void setRateLimitPolicy(RateLimitPolicy rateLimitPolicy) { this.rateLimitPolicy = rateLimitPolicy; }

    public CircuitBreakerPolicy getCircuitBreakerPolicy() { return circuitBreakerPolicy; }
    public void setCircuitBreakerPolicy(CircuitBreakerPolicy circuitBreakerPolicy) { this.circuitBreakerPolicy = circuitBreakerPolicy; }

    public CachePolicy getCachePolicy() { return cachePolicy; }
    public void setCachePolicy(CachePolicy cachePolicy) { this.cachePolicy = cachePolicy; }

    public HeaderRules getHeaderRules() { return headerRules; }
    public void setHeaderRules(HeaderRules headerRules) { this.headerRules = headerRules; }

    public List<String> getAuthForwardHeaders() { return authForwardHeaders; }
    public void setAuthForwardHeaders(List<String> authForwardHeaders) { this.authForwardHeaders = authForwardHeaders; }

    public boolean isAuditEnabled() { return auditEnabled; }
    public void setAuditEnabled(boolean auditEnabled) { this.auditEnabled = auditEnabled; }

    public String getAuditStore() { return auditStore; }
    public void setAuditStore(String auditStore) { this.auditStore = auditStore; }

    public String getHeaderMatchName() { return headerMatchName; }
    public void setHeaderMatchName(String headerMatchName) { this.headerMatchName = headerMatchName; }

    public String getHeaderMatchValue() { return headerMatchValue; }
    public void setHeaderMatchValue(String headerMatchValue) { this.headerMatchValue = headerMatchValue; }

    public JwtPolicy getJwtPolicy() { return jwtPolicy; }
    public void setJwtPolicy(JwtPolicy jwtPolicy) { this.jwtPolicy = jwtPolicy; }

    public CsrfPolicy getCsrfPolicy() { return csrfPolicy; }
    public void setCsrfPolicy(CsrfPolicy csrfPolicy) { this.csrfPolicy = csrfPolicy; }

    public CspPolicy getCspPolicy() { return cspPolicy; }
    public void setCspPolicy(CspPolicy cspPolicy) { this.cspPolicy = cspPolicy; }

    public HttpValidationPolicy getHttpValidationPolicy() { return httpValidationPolicy; }
    public void setHttpValidationPolicy(HttpValidationPolicy httpValidationPolicy) { this.httpValidationPolicy = httpValidationPolicy; }

    @Override
    public String toString() {
        return "RouteDefinition{id='" + id + "', name='" + name + "', pathPattern='" + pathPattern + "', enabled=" + enabled + '}';
    }
}

