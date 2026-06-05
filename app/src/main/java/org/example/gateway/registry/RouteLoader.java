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
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.mapper.RouteMapper;
import org.example.gateway.routes.model.RouteDto;
import org.example.gateway.storage.routes.RouteStorageProvider;
import org.example.utilities.gateway.model.CspPolicy;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.example.utilities.gateway.model.JwtPolicy;
import org.example.utilities.gateway.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link RouteDefinition} instances from the configured storage source
 * and pushes them into the {@link RouteRegistry}.
 *
 * <p>Supported sources (from {@code gateway.config-source}):
 * <ul>
 *   <li>{@code yaml}                — reads routes from {@code application.yml}</li>
 *   <li>{@code database}            — reads from JDBI via {@link RouteStorageProvider}</li>
 *   <li>{@code eclipse-store-lcl}   — reads from EclipseStore local filesystem</li>
 *   <li>{@code eclipse-store-azure} — reads from EclipseStore on Azure Blob Storage</li>
 * </ul>
 */
public class RouteLoader {

    private static final Logger log = LoggerFactory.getLogger(RouteLoader.class);

    private final GatewayConfig config;
    private final RouteRegistry registry;

    private final ObjectMapper jsonMapper = YamlConfigLoader.jsonMapper();
    private final RouteStorageProvider storageProvider;


    /** Full constructor with explicit storage provider. */
    public RouteLoader(GatewayConfig config, RouteRegistry registry,RouteStorageProvider storageProvider) {
        this.config = config;
        this.registry = registry;
        this.storageProvider = storageProvider;
    }

    /** Loads routes from the configured source and pushes them into the registry. */
    public void load() {
        refreshGlobalJwtPolicy();

        List<RouteDefinition> routes;
        String source = config.getConfigSource();

        if ("yaml".equalsIgnoreCase(source)) {
            routes = loadFromYaml();
        } else {
            routes = loadFromStorageProvider();
        }

        registry.reload(routes);
    }

    // ── Source implementations ────────────────────────────────────────────────

    private List<RouteDefinition> loadFromYaml() {
        log.info("Loading routes from application.yml ({} defined)", config.getRoutes().size());
        return new ArrayList<>(config.getRoutes());
    }

    private List<RouteDefinition> loadFromStorageProvider() {
        List<RouteDao> daos = storageProvider.findAll();
        log.info("Loading {} route(s) from storage provider ({})", daos.size(), config.getConfigSource());
        List<RouteDefinition> defs = new ArrayList<>();
        for (RouteDao dao : daos) {
            try {
                RouteDto dto = RouteMapper.routeDaoToRouteDto(dao);
                String json = jsonMapper.writeValueAsString(dto);
                RouteDefinition def = jsonMapper.readValue(json, RouteDefinition.class);
                defs.add(def);
            } catch (Exception e) {
                log.error("Failed to convert route '{}': {}", dao.id(), e.getMessage());
            }
        }
        return defs;
    }


    /**
     * Re-reads {@code application.yml} and pushes updated global security policies
     * into the registry.  Silently skips on parse errors.
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
}
