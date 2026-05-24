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
 * CSRF protection policy attached to a single route.
 *
 * <p>When enabled, all non-safe HTTP methods (POST, PUT, DELETE, PATCH) must
 * include a valid CSRF token either in the {@code X-CSRF-Token} header or as a
 * {@code _csrf} form field.  Tokens are distributed via Hazelcast so multiple
 * gateway replicas share state.
 */
public class CsrfPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /** Time-to-live for generated tokens in seconds (default 1 hour). */
    @JsonProperty("token-ttl-seconds")
    private int tokenTtlSeconds = 3600;

    /**
     * How to bind the CSRF token to the client:
     * {@code IP} (default) or {@code SESSION} (uses Javalin session ID).
     */
    @JsonProperty("bind-to")
    private String bindTo = "IP";

    /**
     * Paths excluded from CSRF validation (prefix match).
     * Useful for public API endpoints and webhook receivers that cannot set
     * a CSRF header (e.g. third-party POST-backs).
     */
    @JsonProperty("exclude-paths")
    private List<String> excludePaths = new ArrayList<>();

    // --- Getters & setters -------------------------------------------------------

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(int tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }

    public String getBindTo() { return bindTo; }
    public void setBindTo(String bindTo) { this.bindTo = bindTo; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
}

