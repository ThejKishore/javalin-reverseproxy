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
 * HTTP input validation policy attached to a single route.
 *
 * <p>When enabled, the {@code HttpValidationFilter} runs OWASP ESAPI blacklist
 * checks against configurable request parts. Blacklist regex patterns are loaded
 * from the {@code validation_rules} database table and can be refreshed at runtime
 * without restarting the server.
 */
public class HttpValidationPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /** Validate query parameters against blacklist patterns. */
    @JsonProperty("validate-query-params")
    private boolean validateQueryParams = true;

    /** Validate request headers against blacklist patterns. */
    @JsonProperty("validate-headers")
    private boolean validateHeaders = true;

    /** Validate cookie values against blacklist patterns. */
    @JsonProperty("validate-cookies")
    private boolean validateCookies = true;

    /**
     * Validate the request body against blacklist patterns.
     * This reads the body into memory — disable for large binary uploads.
     */
    @JsonProperty("validate-body")
    private boolean validateBody = false;

    /**
     * Paths excluded from HTTP validation (prefix match).
     * Useful for binary upload endpoints.
     */
    @JsonProperty("exclude-paths")
    private List<String> excludePaths = new ArrayList<>();

    // --- Getters & setters -------------------------------------------------------

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isValidateQueryParams() { return validateQueryParams; }
    public void setValidateQueryParams(boolean validateQueryParams) { this.validateQueryParams = validateQueryParams; }

    public boolean isValidateHeaders() { return validateHeaders; }
    public void setValidateHeaders(boolean validateHeaders) { this.validateHeaders = validateHeaders; }

    public boolean isValidateCookies() { return validateCookies; }
    public void setValidateCookies(boolean validateCookies) { this.validateCookies = validateCookies; }

    public boolean isValidateBody() { return validateBody; }
    public void setValidateBody(boolean validateBody) { this.validateBody = validateBody; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
}

