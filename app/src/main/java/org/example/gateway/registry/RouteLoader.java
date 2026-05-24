/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gateway.config.GatewayConfig;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.db.RouteDao;
import org.example.gateway.db.RouteRow;
import org.example.utilities.gateway.model.CspPolicy;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.example.utilities.gateway.model.JwtPolicy;
import org.example.utilities.gateway.model.RouteDefinition;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link RouteDefinition} instances from either the YAML config or the
 * database, then pushes them into the {@link RouteRegistry}.
 */
public class RouteLoader {

    private static final Logger log = LoggerFactory.getLogger(RouteLoader.class);

    private final GatewayConfig config;
    private final RouteRegistry registry;
    private final Jdbi jdbi;
    private final ObjectMapper jsonMapper = YamlConfigLoader.jsonMapper();

    public RouteLoader(GatewayConfig config, RouteRegistry registry, Jdbi jdbi) {
        this.config = config;
        this.registry = registry;
        this.jdbi = jdbi;
    }

    /** Loads routes and pushes them into the registry. */
    public void load() {
        // Re-read YAML on every load so that changes to the global jwt-policy
        // (e.g. exclude-paths) take effect without a server restart.
        refreshGlobalJwtPolicy();

        List<RouteDefinition> routes = "database".equalsIgnoreCase(config.getConfigSource())
                ? loadFromDatabase()
                : loadFromYaml();
        registry.reload(routes);
    }

    /**
     * Re-reads {@code application.yml} and pushes updated global security policies
     * (JWT, CSRF, CSP) into the registry.  Silently skips on parse errors so a
     * single bad YAML edit cannot disable the gateway.
     */
    private void refreshGlobalJwtPolicy() {
        try {
            GatewayConfig fresh = new YamlConfigLoader().load();

            JwtPolicy freshJwt = fresh.getJwtPolicy();
            registry.setGlobalJwtPolicy(freshJwt);
            if (freshJwt != null && freshJwt.isEnabled()) {
                log.debug("Global JWT policy refreshed — excludePaths={}", freshJwt.getExcludePaths());
            }

            CsrfPolicy freshCsrf = fresh.getCsrfPolicy();
            registry.setGlobalCsrfPolicy(freshCsrf);
            if (freshCsrf != null && freshCsrf.isEnabled()) {
                log.debug("Global CSRF policy refreshed — excludePaths={}", freshCsrf.getExcludePaths());
            }

            CspPolicy freshCsp = fresh.getCspPolicy();
            registry.setGlobalCspPolicy(freshCsp);
            if (freshCsp != null && freshCsp.isEnabled()) {
                log.debug("Global CSP policy refreshed — policy='{}'", freshCsp.getPolicy());
            }
        } catch (Exception e) {
            log.warn("Failed to refresh global security policies from YAML — keeping previous: {}", e.getMessage());
        }
    }

    // ── Source implementations ────────────────────────────────────────────────

    private List<RouteDefinition> loadFromYaml() {
        log.info("Loading routes from application.yml ({} defined)",
                config.getRoutes().size());
        return new ArrayList<>(config.getRoutes());
    }

    private List<RouteDefinition> loadFromDatabase() {
        return jdbi.withExtension(RouteDao.class, dao -> {
            List<RouteRow> rows = dao.findAll();
            log.info("Loading {} route(s) from database", rows.size());
            List<RouteDefinition> defs = new ArrayList<>();
            for (RouteRow row : rows) {
                try {
                    RouteDefinition def = jsonMapper.readValue(row.getConfigJson(), RouteDefinition.class);
                    def.setEnabled(row.isEnabled());
                    defs.add(def);
                } catch (Exception e) {
                    log.error("Failed to deserialise route '{}': {}", row.getId(), e.getMessage());
                }
            }
            return defs;
        });
    }
}

