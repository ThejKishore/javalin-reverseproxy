/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.mapper.RouteMapper;
import org.example.gateway.routes.model.RouteDto;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RouteStorageProvider} backed by JDBI (H2 / PostgreSQL).
 *
 * <p>Routes are stored as JSON in the {@code config_json} column using the
 * kebab-case {@link RouteDto} format (consistent with the legacy
 * {@code RouteDefinition} serialisation).  On read, the JSON is round-tripped
 * through {@link RouteDto} → {@link RouteDao}.
 */
public class DatabaseRouteStorageProvider implements RouteStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRouteStorageProvider.class);

    private final Jdbi jdbi;
    private final ObjectMapper jsonMapper;

    public DatabaseRouteStorageProvider(Jdbi jdbi) {
        this.jdbi = jdbi;
        this.jsonMapper = YamlConfigLoader.jsonMapper();
    }

    @Override
    public List<RouteDao> findAll() {
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.findAll().stream()
                        .map(this::rowToRouteDao)
                        .filter(Objects::nonNull)
                        .toList());
    }

    @Override
    public List<RouteDao> findAllEnabled() {
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.findAllEnabled().stream()
                        .map(this::rowToRouteDao)
                        .filter(Objects::nonNull)
                        .toList());
    }

    @Override
    public Optional<RouteDao> findById(String id) {
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.findById(id).map(this::rowToRouteDao));
    }

    @Override
    public void insert(RouteDao route) {
        String json = toJson(route);
        jdbi.useExtension(RouteJdbi.class, dao ->
                dao.insert(route.id(), route.name(), json, route.enabled()));
    }

    @Override
    public int update(RouteDao route) {
        String json = toJson(route);
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.update(route.id(), route.name(), json, route.enabled()));
    }

    @Override
    public int setEnabled(String id, boolean enabled) {
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.setEnabled(id, enabled));
    }

    @Override
    public int deleteById(String id) {
        return jdbi.withExtension(RouteJdbi.class, dao ->
                dao.deleteById(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Deserialise a {@link RouteRow}'s JSON column via {@link RouteDto} → {@link RouteDao}. */
    private RouteDao rowToRouteDao(RouteRow row) {
        try {
            RouteDto dto = jsonMapper.readValue(row.getConfigJson(), RouteDto.class);
            // Honour the DB-level enabled flag (it may differ from the JSON payload)
            if (dto.enabled() != row.isEnabled()) {
                dto = new RouteDto(
                        dto.stripPrefix(), dto.pathPattern(), dto.timeoutMs(), dto.targets(),
                        row.isEnabled(), dto.headerRules(), dto.circuitBreakerPolicy(),
                        dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                        dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                        dto.id(), dto.auditEnabled());
            }
            return RouteMapper.routeDtoToRouteDao(dto);
        } catch (Exception e) {
            log.error("Failed to deserialise route '{}': {}", row.getId(), e.getMessage());
            return null;
        }
    }

    /** Serialise a {@link RouteDao} to JSON via {@link RouteDto} to preserve kebab-case keys. */
    private String toJson(RouteDao route) {
        try {
            RouteDto dto = RouteMapper.routeDaoToRouteDto(route);
            return jsonMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise route '" + route.id() + "' to JSON", e);
        }
    }
}
