/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.security;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.HttpClient;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.example.utilities.gateway.security.CsrfGatewayFilter;
import org.example.utilities.gateway.security.CsrfTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CsrfGatewayFilter} using an in-memory token store.
 */
class CsrfGatewayFilterTest {

    /** Simple in-memory token store for testing (no Hazelcast needed). */
    static class InMemoryCsrfTokenStore implements CsrfTokenStore {
        private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

        @Override public void store(String key, String token, int ttl) { store.put(key, token); }
        @Override public String get(String key) { return store.get(key); }
        @Override public void invalidate(String key) { store.remove(key); }

        public void seed(String key, String token) { store.put(key, token); }
    }

    private InMemoryCsrfTokenStore tokenStore;
    private CsrfPolicy policy;

    @BeforeEach
    void setUp() {
        tokenStore = new InMemoryCsrfTokenStore();
        policy = new CsrfPolicy();
        policy.setEnabled(true);
        policy.setTokenTtlSeconds(3600);
    }

    private Javalin buildApp() {
        CsrfGatewayFilter filter = new CsrfGatewayFilter(policy, tokenStore);
        return Javalin.create(cfg -> {
            cfg.routes.get("/form", ctx -> {
                try { filter.filter(new FilterContext(ctx, null), fc -> ctx.result("OK")); }
                catch (Exception e) { ctx.status(500); }
            });
            cfg.routes.post("/form", ctx -> {
                try { filter.filter(new FilterContext(ctx, null), fc -> ctx.result("POSTED")); }
                catch (Exception e) { ctx.status(500); }
            });
        });
    }

    @Test
    void GET_returns_200_and_sets_csrf_token_header() {
        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.get("/form");
            assertThat(resp.code()).isEqualTo(200);
            assertThat(resp.headers().get("X-CSRF-Token")).isNotEmpty();
        });
    }

    @Test
    void POST_returns_403_when_csrf_token_missing() {
        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/form", "{}");
            assertThat(resp.code()).isEqualTo(403);
        });
    }

    @Test
    void POST_returns_403_when_csrf_token_not_found_in_store() {
        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/form", "{}", req ->
                    req.header("X-CSRF-Token", "non-existent-token"));
            assertThat(resp.code()).isEqualTo(403);
            assertThat(resp.body().string()).contains("expired");
        });
    }

    @Test
    void POST_returns_403_when_csrf_token_mismatch() {
        JavalinTest.test(buildApp(), (server, client) -> {
            tokenStore.seed("127.0.0.1", "correct-token");
            var resp = client.post("/form", "{}", req ->
                    req.header("X-CSRF-Token", "wrong-token"));
            assertThat(resp.code()).isEqualTo(403);
            assertThat(resp.body().string()).contains("mismatch");
        });
    }

    @Test
    void POST_returns_200_when_csrf_token_valid() {
        JavalinTest.test(buildApp(), (server, client) -> {
            tokenStore.seed("127.0.0.1", "valid-token");
            var resp = client.post("/form", "{}", req ->
                    req.header("X-CSRF-Token", "valid-token"));
            assertThat(resp.code()).isEqualTo(200);
            assertThat(resp.body().string()).isEqualTo("POSTED");
        });
    }

    @Test
    void POST_returns_200_and_rotates_token_after_valid_use() {
        JavalinTest.test(buildApp(), (server, client) -> {
            tokenStore.seed("127.0.0.1", "one-time-token");
            var resp1 = client.post("/form", "{}", req ->
                    req.header("X-CSRF-Token", "one-time-token"));
            assertThat(resp1.code()).isEqualTo(200);
            var resp2 = client.post("/form", "{}", req ->
                    req.header("X-CSRF-Token", "one-time-token"));
            assertThat(resp2.code()).isEqualTo(403);
        });
    }

    @Test
    void POST_bypasses_csrf_for_excluded_path() {
        policy.setExcludePaths(java.util.List.of("/form"));
        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/form", "{}");
            assertThat(resp.code()).isEqualTo(200);
        });
    }

    @Test
    void GET_does_not_require_csrf_token() {
        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.get("/form");
            assertThat(resp.code()).isEqualTo(200);
        });
    }
}

