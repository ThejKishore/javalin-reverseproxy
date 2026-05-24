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
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.model.CspPolicy;
import org.example.utilities.gateway.security.CspGatewayFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CspGatewayFilter}.
 */
class CspGatewayFilterTest {

    private Javalin buildApp(CspPolicy policy) {
        CspGatewayFilter filter = new CspGatewayFilter(policy);
        return Javalin.create(cfg -> cfg.routes.get("/page", ctx -> {
            try { filter.filter(new FilterContext(ctx, null), fc -> ctx.result("body")); }
            catch (Exception e) { ctx.status(500); }
        }));
    }

    @Test
    void injects_csp_header_on_response() {
        CspPolicy policy = new CspPolicy();
        policy.setEnabled(true);
        policy.setPolicy("default-src 'self'");

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/page");
            assertThat(resp.code()).isEqualTo(200);
            assertThat(resp.headers().get("Content-Security-Policy")).isNotEmpty();
            assertThat(resp.headers().get("Content-Security-Policy").get(0)).isEqualTo("default-src 'self'");
        });
    }

    @Test
    void uses_report_only_header_when_configured() {
        CspPolicy policy = new CspPolicy();
        policy.setEnabled(true);
        policy.setPolicy("default-src 'self'");
        policy.setReportOnly(true);

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/page");
            assertThat(resp.headers().get("Content-Security-Policy-Report-Only")).isNotEmpty();
            assertThat(resp.headers().get("Content-Security-Policy")).isNull();
        });
    }

    @Test
    void does_not_inject_csp_for_excluded_path() {
        CspPolicy policy = new CspPolicy();
        policy.setEnabled(true);
        policy.setPolicy("default-src 'self'");
        policy.setExcludePaths(java.util.List.of("/page"));

        JavalinTest.test(buildApp(policy), (server, client) -> {
            var resp = client.get("/page");
            assertThat(resp.headers().get("Content-Security-Policy")).isNull();
        });
    }
}

