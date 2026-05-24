/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.proxy;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.example.utilities.gateway.model.TargetDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: Javalin gateway → MockWebServer upstream.
 */
class ProxyHandlerIntegrationTest {

    MockWebServer upstream;

    @BeforeEach
    void startUpstream() throws Exception {
        upstream = new MockWebServer();
        upstream.start();
    }

    @AfterEach
    void stopUpstream() throws Exception {
        upstream.shutdown();
    }

    private Javalin buildGateway(String upstreamUrl) {
        RouteRegistry registry = new RouteRegistry();
        RouteDefinition route = new RouteDefinition();
        route.setId("test");
        route.setName("Test Route");
        route.setPathPattern("/proxy");
        route.setEnabled(true);
        route.setTimeoutMs(3000);
        route.setTargets(List.of(new TargetDefinition(upstreamUrl, 1)));
        registry.reload(List.of(route));

        ProxyHandler handler = new ProxyHandler(registry, null);

        return Javalin.create(cfg ->
                cfg.routes.apiBuilder(() -> {
                    io.javalin.apibuilder.ApiBuilder.get("/<path>", handler::handle);
                    io.javalin.apibuilder.ApiBuilder.post("/<path>", handler::handle);
                    io.javalin.apibuilder.ApiBuilder.get("/", handler::handle);
                })
        );
    }

    @Test
    void forwardsGetRequest_andReturnsUpstreamResponse() throws Exception {
        upstream.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"msg\":\"hello\"}")
                .addHeader("Content-Type", "application/json"));

        String upstreamUrl = upstream.url("/").toString();
        // Strip trailing slash
        upstreamUrl = upstreamUrl.endsWith("/") ? upstreamUrl.substring(0, upstreamUrl.length() - 1) : upstreamUrl;

        JavalinTest.test(buildGateway(upstreamUrl), (server, client) -> {
            var resp = client.get("/proxy");
            assertEquals(200, resp.code());
            assertTrue(resp.body().string().contains("hello"));
        });
    }

    @Test
    void upstreamReturns404_gatewayForwards404() throws Exception {
        upstream.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

        String upstreamUrl = upstream.url("/").toString();
        upstreamUrl = upstreamUrl.endsWith("/") ? upstreamUrl.substring(0, upstreamUrl.length() - 1) : upstreamUrl;

        JavalinTest.test(buildGateway(upstreamUrl), (server, client) -> {
            var resp = client.get("/proxy/missing");
            assertEquals(404, resp.code());
        });
    }

    @Test
    void noRouteMatch_returns404() throws Exception {
        JavalinTest.test(buildGateway("http://localhost:9999"), (server, client) -> {
            var resp = client.get("/no-route-here");
            assertEquals(404, resp.code());
        });
    }
}

