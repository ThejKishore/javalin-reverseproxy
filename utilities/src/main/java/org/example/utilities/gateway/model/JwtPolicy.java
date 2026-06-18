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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT validation policy attached to a single route.
 *
 * <p>When enabled, inbound requests must carry a valid Bearer JWT in the
 * {@code Authorization} header. Requests whose path starts with an entry in
 * {@code excludePaths} bypass JWT validation entirely.
 */
public class JwtPolicy {

    @JsonProperty("enabled")
    private boolean enabled = false;

    /**
     * Signing algorithm: {@code HS256} (shared-secret) or {@code RS256}
     * (RSA public-key).
     */
    @JsonProperty("algorithm")
    private String algorithm = "HS256";

    /**
     * For HS256: the shared secret string.
     * For RS256: the PEM-encoded RSA public key (or a file path prefixed with
     * {@code file:}).
     */
    @JsonProperty("secret-or-public-key")
    private String secretOrPublicKey;

    /** Expected {@code iss} claim value (optional — skipped when blank). */
    @JsonProperty("issuer")
    private String issuer;

    /** Expected {@code aud} claim value (optional — skipped when blank). */
    @JsonProperty("audience")
    private String audience;

    /**
     * Additional claims that must be present and match the given value in the
     * JWT payload, e.g. {@code role: admin}.
     */
    @JsonProperty("required-claims")
    private Map<String, String> requiredClaims = new LinkedHashMap<>();

    /**
     * Request paths that are excluded from JWT validation (prefix match).
     * Example: {@code /api/public} will bypass check for any sub-path.
     */
    @JsonProperty("exclude-paths")
    private List<String> excludePaths = new ArrayList<>();

    // --- Getters & setters -------------------------------------------------------

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getSecretOrPublicKey() { return secretOrPublicKey; }
    public void setSecretOrPublicKey(String secretOrPublicKey) { this.secretOrPublicKey = secretOrPublicKey; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public Map<String, String> getRequiredClaims() { return requiredClaims; }
    public void setRequiredClaims(Map<String, String> requiredClaims) { this.requiredClaims = requiredClaims; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
}

