/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.mapper.RouteMapper;
import org.example.gateway.routes.model.RouteDto;
import org.example.gateway.storage.azuretable.AzureTableClientFactory;
import org.example.utilities.gateway.exception.GatewayException;
import org.example.gateway.routes.helper.AzureTableRouteKeyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link RouteStorageProvider} backed by Azure Table Storage.
 *
 * <p>Entity mapping:
 * <ul>
 *   <li>PartitionKey = {@code "routes"} — all routes share one partition for simplicity.</li>
 *   <li>RowKey       = route {@code path-pattern}.</li>
 *   <li>Properties   = {@code name}, {@code configJson} (full RouteDto JSON), {@code enabled}, {@code version}.</li>
 * </ul>
 *
 * <p>Activated when {@code gateway.config-source = azure-table}.
 */
public class AzureTableRouteStorageProvider implements RouteStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureTableRouteStorageProvider.class);
    private static final String PARTITION = "routes";

    private static final String COL_NAME        = "name";
    private static final String COL_CONFIG_JSON = "configJson";
    private static final String COL_ENABLED     = "enabled";
    private static final String COL_VERSION     = "version";

    private final TableClient tableClient;
    private final ObjectMapper jsonMapper;

    public AzureTableRouteStorageProvider(AzureTableClientFactory factory) {
        this.tableClient = factory.routesClient();
        this.jsonMapper  = YamlConfigLoader.jsonMapper();
        log.info("AzureTableRouteStorageProvider initialised (table={})", tableClient.getTableName());
    }

    @Override
    public List<RouteDao> findAll() {
        List<RouteDao> results = new ArrayList<>();
        tableClient.listEntities().forEach(entity -> {
            RouteDao dao = entityToDao(entity);
            if (dao != null) results.add(dao);
        });
        return results;
    }

    @Override
    public List<RouteDao> findAllEnabled() {
        String filter = String.format("%s eq true", COL_ENABLED);
        ListEntitiesOptions options = new ListEntitiesOptions().setFilter(filter);
        List<RouteDao> results = new ArrayList<>();
        tableClient.listEntities(options, null, null).forEach(entity -> {
            RouteDao dao = entityToDao(entity);
            if (dao != null) results.add(dao);
        });
        return results;
    }

    @Override
    public Optional<RouteDao> findById(String id) {
        try {
            TableEntity entity = tableClient.getEntity(PARTITION, id);
            return Optional.ofNullable(entityToDao(entity));
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return Optional.empty();
            throw e;
        }
    }

    @Override
    public void insert(RouteDao route) {
        if (findById(route.pathPattern()).isPresent()) {
            throw new GatewayException("Duplicate route path-pattern: " + route.pathPattern(), 409);
        }
        TableEntity entity = toEntity(route);
        tableClient.createEntity(entity);
        log.debug("Azure Table route: inserted pathPattern={}", route.pathPattern());
    }

    @Override
    public int update(RouteDao route) {
        try {
            TableEntity entity = tableClient.getEntity(PARTITION, route.id());
            long currentVersion = toLong(entity.getProperty(COL_VERSION));
            if (currentVersion != route.version()) {
                throw new GatewayException("Route version conflict for path-pattern: " + route.pathPattern(), 409);
            }
            entity.addProperty(COL_NAME, route.name());
            entity.addProperty(COL_CONFIG_JSON, toJson(route));
            entity.addProperty(COL_ENABLED, route.enabled());
            entity.addProperty(COL_VERSION, route.version() + 1);
            tableClient.updateEntity(entity);
            log.debug("Azure Table route: updated pathPattern={}", route.pathPattern());
            return 1;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return 0;
            throw e;
        }
    }

    @Override
    public int setEnabled(String id, long version, boolean enabled) {
        try {
            TableEntity entity = tableClient.getEntity(PARTITION, id);
            long currentVersion = toLong(entity.getProperty(COL_VERSION));
            if (currentVersion != version) {
                throw new GatewayException("Route version conflict for path-pattern: " + id, 409);
            }
            entity.addProperty(COL_ENABLED, enabled);
            entity.addProperty(COL_VERSION, version + 1);
            tableClient.updateEntity(entity);
            log.debug("Azure Table route: setEnabled pathPattern={} enabled={}", id, enabled);
            return 1;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return 0;
            throw e;
        }
    }

    @Override
    public int deleteById(String id) {
        try {
            tableClient.deleteEntity(PARTITION, id);
            log.debug("Azure Table route: deleted pathPattern={}", id);
            return 1;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return 0;
            throw e;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TableEntity toEntity(RouteDao route) {
        String json = toJson(route);
        TableEntity entity = new TableEntity(PARTITION, org.example.gateway.routes.helper.AzureTableRouteKeyHelper.encodeRowKey(route.pathPattern()));
        entity.addProperty(COL_NAME, route.name());
        entity.addProperty("pathPattern", route.pathPattern());
        entity.addProperty(COL_CONFIG_JSON, json);
        entity.addProperty(COL_ENABLED, route.enabled());
        entity.addProperty(COL_VERSION, route.version() > 0 ? route.version() : 1L);
        return entity;
    }

    private RouteDao entityToDao(TableEntity entity) {
        try {
            String json = (String) entity.getProperty(COL_CONFIG_JSON);
            boolean enabled = Boolean.TRUE.equals(entity.getProperty(COL_ENABLED));
            RouteDto dto = jsonMapper.readValue(json, RouteDto.class);
            long rowVersion = toLong(entity.getProperty(COL_VERSION));
            String pathPattern = org.example.gateway.routes.helper.AzureTableRouteKeyHelper.decodeRowKey(entity.getRowKey());
            if (dto.enabled() != enabled || !pathPattern.equals(dto.pathPattern()) || dto.version() != rowVersion) {
                dto = new RouteDto(
                        dto.stripPrefix(), pathPattern, dto.timeoutMs(), dto.targets(),
                        enabled, dto.headerRules(), dto.circuitBreakerPolicy(),
                        dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                        dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                        dto.id(), dto.auditEnabled(), rowVersion, dto.metaData());
            }
            return RouteMapper.routeDtoToRouteDao(dto);
        } catch (Exception e) {
            log.error("Failed to deserialise route entity '{}': {}", entity.getRowKey(), e.getMessage());
            return null;
        }
    }

    private String toJson(RouteDao route) {
        try {
            RouteDto dto = RouteMapper.routeDaoToRouteDto(route);
            return jsonMapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise route '" + route.id() + "' to JSON", e);
        }
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

}
