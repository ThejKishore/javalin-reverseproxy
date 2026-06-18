/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.security;

import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.testtools.JavalinTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.JwtPolicy;
import org.example.utilities.gateway.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit / integration tests for {@link JwtAuthFilter} using {@code JavalinTest}.
 */
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-minimum-32-chars!!";
    private static final SecretKey KEY  = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private String validToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY)
                .compact();
    }

    private String expiredToken() {
        return Jwts.builder()
                .subject("user")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(KEY)
                .compact();
    }

    private String tokenWithClaim(String claimKey, String claimValue) {
        return Jwts.builder()
                .subject("user")
                .claim(claimKey, claimValue)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY)
                .compact();
    }

    private Javalin buildApp(JwtPolicy policy) {
        JwtAuthFilter filter = new JwtAuthFilter(policy);
        return Javalin.create(cfg -> cfg.routes.get("/secure", ctx -> {
            try {
                FilterContext fc = new FilterContext(ctx, null);
                filter.filter(fc, chain -> ctx.result("OK"));
            } catch (HttpResponseException e) {
                throw e;
            } catch (Exception e) {
                ctx.status(500).result(e.getMessage());
            }
        }));
    }

    @Test
    void returns401_when_authorization_header_missing() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/secure");
            assertThat(resp.code()).isEqualTo(401);
        });
    }

    @Test
    void returns401_when_token_expired() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/secure", req -> req.header("Authorization", "Bearer " + expiredToken()));
            assertThat(resp.code()).isEqualTo(401);
            assertThat(resp.body().string()).contains("expired");
        });
    }

    @Test
    void returns401_when_token_signature_invalid() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/secure", req ->
                    req.header("Authorization", "Bearer not.a.valid.jwt"));
            assertThat(resp.code()).isEqualTo(401);
        });
    }

    @Test
    void returns200_when_token_valid() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/secure",
                    req -> req.header("Authorization", "Bearer " + validToken("alice")));
            assertThat(resp.code()).isEqualTo(200);
            assertThat(resp.body().string()).isEqualTo("OK");
        });
    }

    @Test
    void returns403_when_required_claim_missing() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);
        policy.setRequiredClaims(Map.of("role", "admin"));

        JavalinTest.test(buildApp(policy), (server, client) -> {
            // Token present but no 'role' claim
            var resp = client.get("/secure",
                    req -> req.header("Authorization", "Bearer " + validToken("user")));
            assertThat(resp.code()).isEqualTo(403);
        });
    }

    @Test
    void returns200_when_required_claim_satisfied() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);
        policy.setRequiredClaims(Map.of("role", "admin"));

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/secure", req ->
                    req.header("Authorization", "Bearer " + tokenWithClaim("role", "admin")));
            assertThat(resp.code()).isEqualTo(200);
        });
    }

    @Test
    void bypasses_excluded_path_without_token() {
        JwtPolicy policy = new JwtPolicy();
        policy.setEnabled(true);
        policy.setSecretOrPublicKey(SECRET);
        policy.setExcludePaths(java.util.List.of("/secure"));

        JavalinTest.test(buildApp(policy), (server, client) -> {
            // No header — but path is excluded
            var resp = client.get("/secure");
            assertThat(resp.code()).isEqualTo(200);
        });
    }
}

