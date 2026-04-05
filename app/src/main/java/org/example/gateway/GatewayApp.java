/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway;

import io.javalin.Javalin;
import org.example.gateway.api.AdminController;
import org.example.gateway.api.HealthController;
import org.example.gateway.config.GatewayConfig;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.db.DatabaseManager;
import org.example.gateway.proxy.ProxyHandler;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.exception.CircuitBreakerOpenException;
import org.example.utilities.gateway.exception.GatewayException;
import org.example.utilities.gateway.exception.NoTargetAvailableException;
import org.example.utilities.gateway.exception.RateLimitExceededException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Entry point for the Javalin API Gateway.
 *
 * <p>Bootstraps configuration → database → route registry → Javalin server.
 * A background scheduler periodically re-loads routes from the configured
 * source (YAML file or database).
 */
public class GatewayApp {

    private static final Logger log = LoggerFactory.getLogger(GatewayApp.class);

    public static void main(String[] args) throws Exception {
        GatewayConfig config = new YamlConfigLoader().load();

        // Database + JDBI
        DatabaseManager db = new DatabaseManager(config.getDatasource());
        Jdbi jdbi = db.getJdbi();

        // Registry + loader
        RouteRegistry registry = new RouteRegistry();
        RouteLoader loader = new RouteLoader(config, registry, jdbi);
        loader.load();

        // Handlers & controllers
        ProxyHandler proxyHandler = new ProxyHandler(registry, jdbi);
        AdminController admin = new AdminController(registry, loader, jdbi);
        HealthController health = new HealthController(registry);

        // ── Build Javalin app ─────────────────────────────────────────────────
        Javalin app = Javalin.create(cfg -> {
            cfg.bundledPlugins.enableDevLogging();
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));

            // ── Health endpoints ──────────────────────────────────────────────
            cfg.routes.apiBuilder(() ->
                path("/gateway/health", () -> {
                    get(health::health);
                    get("/live",  health::liveness);
                    get("/ready", health::readiness);
                })
            );

            // ── Admin API ─────────────────────────────────────────────────
            cfg.routes.apiBuilder(() ->
                path("/gateway/admin", () -> {
                    post("/reload", admin::reload);
                    get("/audit/logs", admin::getAuditLogs);
                    get("/change-logs", admin::getChangeLogs);
                    path("/audit/routes/{routeId}", () -> {
                        get(admin::getRouteAuditLogs);
                    });
                    path("/routes", () -> {
                        get(admin::listRoutes);
                        post(admin::createRoute);
                        path("/{id}", () -> {
                            get(admin::getRoute);
                            put(admin::updateRoute);
                            delete(admin::deleteRoute);
                            patch("/enable",  admin::enableRoute);
                            patch("/disable", admin::disableRoute);
                        });
                    });
                })
            );

            // ── Catch-all HTTP proxy (all verbs, slash-spanning param) ─────────
            cfg.routes.get("/<path>",     proxyHandler::handle);
            cfg.routes.post("/<path>",    proxyHandler::handle);
            cfg.routes.put("/<path>",     proxyHandler::handle);
            cfg.routes.patch("/<path>",   proxyHandler::handle);
            cfg.routes.delete("/<path>",  proxyHandler::handle);
            cfg.routes.get("/",           proxyHandler::handle);
            cfg.routes.post("/",          proxyHandler::handle);


            // ── Exception handlers ────────────────────────────────────────────
            cfg.routes.exception(RateLimitExceededException.class, (e, ctx) ->
                    ctx.status(429).json(errorBody(429, e.getMessage())));
            cfg.routes.exception(CircuitBreakerOpenException.class, (e, ctx) ->
                    ctx.status(503).json(errorBody(503, e.getMessage())));
            cfg.routes.exception(NoTargetAvailableException.class, (e, ctx) ->
                    ctx.status(503).json(errorBody(503, e.getMessage())));
            cfg.routes.exception(GatewayException.class, (e, ctx) ->
                    ctx.status(e.getHttpStatus()).json(errorBody(e.getHttpStatus(), e.getMessage())));
            cfg.routes.exception(SocketTimeoutException.class, (e, ctx) ->
                    ctx.status(504).json(errorBody(504, "Upstream timeout: " + e.getMessage())));
            cfg.routes.exception(java.io.IOException.class, (e, ctx) ->
                    ctx.status(502).json(errorBody(502, "Bad gateway: " + e.getMessage())));
        }).start(config.getPort());

        log.info("Javalin Gateway started on port {}", config.getPort());

        // ── Scheduled route refresh ───────────────────────────────────────────
        int interval = config.getRefreshIntervalSeconds();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gateway-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.debug("Refreshing routes from source '{}'", config.getConfigSource());
                loader.load();
            } catch (Exception ex) {
                log.error("Route refresh failed: {}", ex.getMessage(), ex);
            }
        }, interval, interval, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            app.stop();
            log.info("Gateway stopped");
        }));
    }

    private static Map<String, Object> errorBody(int status, String message) {
        return Map.of("status", status, "error", message);
    }
}

