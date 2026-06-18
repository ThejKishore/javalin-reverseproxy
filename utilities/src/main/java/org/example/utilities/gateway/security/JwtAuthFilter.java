/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.utilities.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.example.utilities.gateway.model.JwtPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * GatewayFilter that validates a Bearer JWT token on each inbound request.
 *
 * <p>The filter is placed first in the chain so unauthenticated requests are
 * rejected immediately, before any expensive upstream call.
 *
 * <ul>
 *   <li>Missing or malformed {@code Authorization} header → HTTP 401</li>
 *   <li>Expired, invalid signature, or unknown issuer → HTTP 401</li>
 *   <li>Valid token but required claims not satisfied → HTTP 403</li>
 *   <li>Path matches {@code excludePaths} list → bypass (pass through)</li>
 * </ul>
 */
public class JwtAuthFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtPolicy policy;
    private final JwtParser parser;

    public JwtAuthFilter(JwtPolicy policy) {
        this.policy = policy;
        this.parser = buildParser(policy);
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        String path = ctx.getJavalinCtx().path();

        // Bypass for excluded paths
        if (isExcluded(path)) {
            chain.proceed(ctx);
            return;
        }

        String authHeader = ctx.getJavalinCtx().header("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedResponse("Missing or malformed Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).strip();
        Jws<Claims> jws;
        try {
            jws = parser.parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired for path={}: {}", path, e.getMessage());
            throw new UnauthorizedResponse("Token has expired");
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            log.debug("JWT invalid for path={}: {}", path, e.getMessage());
            throw new UnauthorizedResponse("Invalid token");
        } catch (JwtException e) {
            log.warn("JWT validation error for path={}: {}", path, e.getMessage());
            throw new UnauthorizedResponse("Token validation failed");
        }

        Claims claims = jws.getPayload();

        // Check required claims
        for (Map.Entry<String, String> required : policy.getRequiredClaims().entrySet()) {
            Object actual = claims.get(required.getKey());
            if (actual == null || !required.getValue().equals(String.valueOf(actual))) {
                log.debug("JWT claim check failed path={} claim={} expected={} actual={}",
                        path, required.getKey(), required.getValue(), actual);
                throw new ForbiddenResponse("Insufficient permissions");
            }
        }

        // Inject subject into filter context attributes for downstream use
        ctx.getJavalinCtx().attribute("jwt.subject", claims.getSubject());
        ctx.getJavalinCtx().attribute("jwt.claims", claims);

        chain.proceed(ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExcluded(String path) {
        for (String prefix : policy.getExcludePaths()) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private static JwtParser buildParser(JwtPolicy policy) {
        JwtParserBuilder builder = Jwts.parser();

        if (policy.getIssuer() != null && !policy.getIssuer().isBlank()) {
            builder.requireIssuer(policy.getIssuer());
        }
        if (policy.getAudience() != null && !policy.getAudience().isBlank()) {
            builder.requireAudience(policy.getAudience());
        }

        String alg = policy.getAlgorithm() == null ? "HS256" : policy.getAlgorithm().toUpperCase();
        String keyMaterial = policy.getSecretOrPublicKey();

        switch (alg) {
            case "RS256" -> {
                try {
                    byte[] decoded = Base64.getDecoder().decode(keyMaterial.replaceAll("\\s", "")
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", ""));
                    java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
                    java.security.PublicKey pub = kf.generatePublic(
                            new java.security.spec.X509EncodedKeySpec(decoded));
                    builder.verifyWith(pub);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid RS256 public key: " + e.getMessage(), e);
                }
            }
            case "HS384" -> {
                // HS384 requires ≥ 384 bits (48 bytes)
                SecretKey key = new SecretKeySpec(
                        ensureMinLength(keyMaterial.getBytes(StandardCharsets.UTF_8), 48, alg),
                        "HmacSHA384");
                builder.verifyWith(key);
            }
            case "HS512" -> {
                // HS512 requires ≥ 512 bits (64 bytes)
                SecretKey key = new SecretKeySpec(
                        ensureMinLength(keyMaterial.getBytes(StandardCharsets.UTF_8), 64, alg),
                        "HmacSHA512");
                builder.verifyWith(key);
            }
            default -> {
                // HS256 — requires ≥ 256 bits (32 bytes)
                SecretKey key = new SecretKeySpec(
                        ensureMinLength(keyMaterial.getBytes(StandardCharsets.UTF_8), 32, alg),
                        "HmacSHA256");
                builder.verifyWith(key);
            }
        }

        return builder.build();
    }

    /**
     * Throws a clear startup error if the key is shorter than required for the
     * configured algorithm, telling the operator exactly what is wrong.
     */
    private static byte[] ensureMinLength(byte[] key, int minBytes, String alg) {
        if (key.length < minBytes) {
            throw new IllegalArgumentException(
                    "JWT secret-or-public-key is too short for " + alg + ": " +
                    "got " + key.length + " bytes, need at least " + minBytes + ". " +
                    "Please update 'secret-or-public-key' in application.yml.");
        }
        return key;
    }

}

