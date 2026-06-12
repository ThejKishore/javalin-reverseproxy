/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway;

import io.javalin.Javalin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.example.gateway.api.AdminController;
import org.example.gateway.api.AuditController;
import org.example.gateway.api.HealthController;
import org.example.gateway.api.ConsolidatedOpenApiController;
import org.example.gateway.config.GatewayConfig;
import org.example.gateway.config.HazelcastConfig;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.config.OpenApiExportConfig;
import org.example.gateway.db.DatabaseManager;
import org.example.gateway.proxy.ProxyHandler;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.gateway.security.HazelcastCsrfTokenStore;
import org.example.gateway.security.ValidationRuleStore;
import org.example.gateway.storage.audit.AuditStorageProvider;
import org.example.gateway.storage.audit.AuditStorageProviderFactory;
import org.example.gateway.storage.changelog.ChangeLogStorageProvider;
import org.example.gateway.storage.changelog.ChangeLogStorageProviderFactory;
import org.example.gateway.storage.routes.RouteStorageProvider;
import org.example.gateway.storage.routes.RouteStorageProviderFactory;
import org.example.gateway.storage.validationrule.ValidationRuleStorageProvider;
import org.example.gateway.storage.validationrule.ValidationRuleStorageProviderFactory;
import org.example.utilities.gateway.exception.CircuitBreakerOpenException;
import org.example.utilities.gateway.exception.GatewayException;
import org.example.utilities.gateway.exception.NoTargetAvailableException;
import org.example.utilities.gateway.exception.RateLimitExceededException;
import org.example.utilities.gateway.security.HttpValidationFilter;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
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

        // ── Hazelcast (CSRF token store) ──────────────────────────────────────
        HazelcastInstance hazelcast = null;
        HazelcastCsrfTokenStore csrfTokenStore = null;
        HazelcastConfig hzCfg = config.getHazelcast();
        if (hzCfg != null && hzCfg.isEnabled()) {
            Config hz = new Config();
            hz.setClusterName(hzCfg.getClusterName());
            hz.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            if (!hzCfg.getMembers().isEmpty()) {
                hz.getNetworkConfig().getJoin().getTcpIpConfig()
                  .setEnabled(true)
                  .setMembers(hzCfg.getMembers());
            }
            hazelcast = Hazelcast.newHazelcastInstance(hz);
            csrfTokenStore = new HazelcastCsrfTokenStore(hazelcast);
            log.info("Hazelcast initialised (cluster={})", hzCfg.getClusterName());
        } else {
            log.info("Hazelcast disabled — CSRF filters will be inactive");
        }

        // ── Validation rule store (hot-reloadable ESAPI patterns) ─────────────
        boolean isYaml = "yaml".equalsIgnoreCase(config.getConfigSource());
        ValidationRuleStorageProvider validationRuleStorageProvider = isYaml
                ? null
                : ValidationRuleStorageProviderFactory.create(config, jdbi);
        ValidationRuleStore validationRuleStore = new ValidationRuleStore(validationRuleStorageProvider);
        validationRuleStore.reload();

        // Adapter: ValidationRuleStore → HttpValidationFilter.HttpValidationRuleProvider
        HttpValidationFilter.HttpValidationRuleProvider ruleProvider = () ->
                validationRuleStore.getRules().stream()
                        .map(r -> new HttpValidationFilter.ValidationRule(
                                r.id(), r.name(), r.target(), r.pattern()))
                        .toList();

        // Registry + loader
        RouteRegistry registry = new RouteRegistry();
        if (csrfTokenStore != null) {
            registry.setCsrfTokenStore(csrfTokenStore);
        }
        registry.setValidationRuleProvider(ruleProvider);

        // Wire global JWT policy (applies to every route unless overridden per-route)
        if (config.getJwtPolicy() != null && config.getJwtPolicy().isEnabled()) {
            registry.setGlobalJwtPolicy(config.getJwtPolicy());
            log.info("Global JWT policy enabled (algorithm={}, excludePaths={})",
                    config.getJwtPolicy().getAlgorithm(),
                    config.getJwtPolicy().getExcludePaths());
        }
        // Wire global CSRF policy
        if (config.getCsrfPolicy() != null && config.getCsrfPolicy().isEnabled()) {
            registry.setGlobalCsrfPolicy(config.getCsrfPolicy());
            log.info("Global CSRF policy enabled (bindTo={}, excludePaths={})",
                    config.getCsrfPolicy().getBindTo(),
                    config.getCsrfPolicy().getExcludePaths());
        }
        // Wire global CSP policy
        if (config.getCspPolicy() != null && config.getCspPolicy().isEnabled()) {
            registry.setGlobalCspPolicy(config.getCspPolicy());
            log.info("Global CSP policy enabled (reportOnly={}, policy='{}')",
                    config.getCspPolicy().isReportOnly(),
                    config.getCspPolicy().getPolicy());
        }

        // ── Storage provider (routes) ─────────────────────────────────────────
        RouteStorageProvider storageProvider = isYaml
                ? null
                : RouteStorageProviderFactory.create(config, jdbi);

        // ── Storage providers (audit, changelog) ──────────────────────────────
        AuditStorageProvider auditProvider = isYaml
                ? null
                : AuditStorageProviderFactory.create(config, jdbi);
        ChangeLogStorageProvider changeLogProvider = isYaml
                ? null
                : ChangeLogStorageProviderFactory.create(config, jdbi);

        RouteLoader loader = new RouteLoader(config, registry, storageProvider);
        loader.load();
        AdminController admin = new AdminController(registry, loader, storageProvider,
                validationRuleStore, changeLogProvider, validationRuleStorageProvider);
        AuditController audit = new AuditController(auditProvider, changeLogProvider);
        HealthController health = new HealthController(registry);
        OpenApiExportConfig openApiExportConfig = config.getOpenApiExport();
        ConsolidatedOpenApiController consolidatedOpenApi =
                new ConsolidatedOpenApiController(registry, openApiExportConfig);

        // Handlers & controllers
        ProxyHandler proxyHandler = new ProxyHandler(registry, jdbi);

        // ── Build Javalin app ─────────────────────────────────────────────────
        Javalin app = Javalin.create(cfg -> {
            cfg.bundledPlugins.enableDevLogging();
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));

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
                    get("/audit/logs", audit::getAuditLogs);
                    get("/change-logs", audit::getChangeLogs);
                    path("/audit/routes/{routeId}", () -> get(audit::getRouteAuditLogs));
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
                    // ── Security: Validation rules ────────────────────────
                    path("/security/rules", () -> {
                        get(admin::listValidationRules);
                        post(admin::createValidationRule);
                        post("/reload", admin::reloadValidationRules);
                        path("/{id}", () -> {
                            patch("/enable",  admin::enableValidationRule);
                            patch("/disable", admin::disableValidationRule);
                            delete(admin::deleteValidationRule);
                        });
                    });
                })
            );

            // Secured consolidated OpenAPI export (for APIM registration)
            cfg.routes.get(openApiExportConfig.getPath(), consolidatedOpenApi::getConsolidatedSpec);

            // ── Catch-all HTTP proxy (all verbs, slash-spanning param) ─────────
            cfg.routes.get("/<path>", proxyHandler);
            cfg.routes.post("/<path>", proxyHandler);
            cfg.routes.put("/<path>", proxyHandler);
            cfg.routes.patch("/<path>", proxyHandler);
            cfg.routes.delete("/<path>", proxyHandler);
            cfg.routes.get("/", proxyHandler);
            cfg.routes.post("/", proxyHandler);


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

        final HazelcastInstance hazelcastRef = hazelcast;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            app.stop();
            if (hazelcastRef != null) hazelcastRef.shutdown();
            log.info("Gateway stopped");
        }));
    }

    private static Map<String, Object> errorBody(int status, String message) {
        return Map.of("status", status, "error", message);
    }
}

