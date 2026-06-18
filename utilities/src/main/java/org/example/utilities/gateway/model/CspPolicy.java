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
 * Content Security Policy attached to a single route.
 *
 * <p>When enabled, the filter injects a {@code Content-Security-Policy} header
 * on every response that passes through this route.
 */
public class CspPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /**
     * The full CSP policy string, e.g.
     * {@code default-src 'self'; script-src 'self' https://cdn.example.com}.
     */
    @JsonProperty("policy")
    private String policy = "default-src 'self'";

    /**
     * When {@code true}, use {@code Content-Security-Policy-Report-Only} header
     * instead of enforcing. Useful for testing new policies.
     */
    @JsonProperty("report-only")
    private boolean reportOnly = false;

    /**
     * Paths excluded from CSP header injection (prefix match).
     * Useful for API paths that don't serve HTML.
     */
    @JsonProperty("exclude-paths")
    private List<String> excludePaths = new ArrayList<>();

    // --- Getters & setters -------------------------------------------------------

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPolicy() { return policy; }
    public void setPolicy(String policy) { this.policy = policy; }

    public boolean isReportOnly() { return reportOnly; }
    public void setReportOnly(boolean reportOnly) { this.reportOnly = reportOnly; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
}

