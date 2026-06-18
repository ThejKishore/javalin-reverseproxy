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
import org.example.utilities.gateway.model.HttpValidationPolicy;
import org.example.utilities.gateway.security.HttpValidationFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpValidationFilter}.
 */
class HttpValidationFilterTest {

    private static final HttpValidationFilter.ValidationRule XSS_RULE =
        new HttpValidationFilter.ValidationRule("xss-01", "XSS script tag",
            "ALL", Pattern.compile("(?i)<script", Pattern.DOTALL));

    private static final HttpValidationFilter.ValidationRule SQLI_RULE =
        new HttpValidationFilter.ValidationRule("sqli-01", "SQL injection",
            "QUERY_PARAM", Pattern.compile("(?i)union\\s+select", Pattern.DOTALL));

    private Javalin buildApp(HttpValidationPolicy policy,
                             List<HttpValidationFilter.ValidationRule> rules) {
        HttpValidationFilter filter = new HttpValidationFilter(policy, () -> rules);
        return Javalin.create(cfg -> cfg.routes.get("/api", ctx -> {
            try { filter.filter(new FilterContext(ctx, null), fc -> ctx.result("OK")); }
            catch (Exception e) { ctx.status(500); }
        }));
    }

    @Test
    void returns_200_when_request_is_clean() {
        HttpValidationPolicy policy = new HttpValidationPolicy();
        policy.setEnabled(true);

        JavalinTest.test(buildApp(policy, List.of(XSS_RULE)), (server, client) -> {
            var resp = client.get("/api?name=Alice");
            assertThat(resp.code()).isEqualTo(200);
        });
    }

    @Test
    void returns_400_when_query_param_contains_xss() {
        HttpValidationPolicy policy = new HttpValidationPolicy();
        policy.setEnabled(true);

        JavalinTest.test(buildApp(policy, List.of(XSS_RULE)), (server, client) -> {
            var resp = client.get("/api?q=%3Cscript%3Ealert(1)%3C/script%3E");
            assertThat(resp.code()).isEqualTo(400);
            assertThat(resp.body().string()).contains("XSS script tag");
        });
    }

    @Test
    void returns_400_when_query_param_contains_sql_injection() {
        HttpValidationPolicy policy = new HttpValidationPolicy();
        policy.setEnabled(true);

        JavalinTest.test(buildApp(policy, List.of(SQLI_RULE)), (server, client) -> {
            var resp = client.get("/api?id=1%20UNION%20SELECT%20*%20FROM%20users");
            assertThat(resp.code()).isEqualTo(400);
        });
    }

    @Test
    void returns_200_for_excluded_path() {
        HttpValidationPolicy policy = new HttpValidationPolicy();
        policy.setEnabled(true);
        policy.setExcludePaths(List.of("/api"));

        JavalinTest.test(buildApp(policy, List.of(XSS_RULE)), (server, client) -> {
            var resp = client.get("/api?q=%3Cscript%3Ealert(1)%3C/script%3E");
            assertThat(resp.code()).isEqualTo(200);
        });
    }

    @Test
    void returns_200_when_no_rules_loaded() {
        HttpValidationPolicy policy = new HttpValidationPolicy();
        policy.setEnabled(true);

        JavalinTest.test(buildApp(policy, List.of()), (server, client) -> {
            var resp = client.get("/api?q=%3Cscript%3Ebad%3C/script%3E");
            assertThat(resp.code()).isEqualTo(200);
        });
    }
}

