/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gateway.routes.model.RouteDto;

import java.io.IOException;

/**
 * Mapper for converting between RouteDto and RouteRow (database model).
 * Handles recursive mapping of nested objects and JSON serialization.
 */
public class RouteMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a RouteDto to a RouteRow JSON string representation.
     * Serializes the entire RouteDto hierarchy to JSON.
     *
     * @param routeDto the RouteDto to convert
     * @return JSON string representation of the RouteDto
     * @throws RuntimeException if serialization fails
     */
    public static String routeDtoToJson(RouteDto routeDto) {
        if (routeDto == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(routeDto);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize RouteDto to JSON", e);
        }
    }

    /**
     * Converts a JSON string to a RouteDto.
     * Deserializes the entire JSON hierarchy into RouteDto with nested objects.
     *
     * @param json the JSON string to convert
     * @return RouteDto deserialized from JSON
     * @throws RuntimeException if deserialization fails
     */
    public static RouteDto jsonToRouteDto(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RouteDto.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize JSON to RouteDto", e);
        }
    }

    /**
     * Converts a RouteDto to a database-compatible format (RouteRow fields).
     * This is a convenience method that extracts top-level properties from RouteDto.
     *
     * @param routeDto the RouteDto to convert
     * @return an object array with [id, name, configJson, enabled]
     */
    public static Object[] routeDtoToRowData(RouteDto routeDto) {
        if (routeDto == null) {
            return new Object[]{null, null, null, false};
        }
        return new Object[]{
                routeDto.id(),
                routeDto.name(),
                routeDtoToJson(routeDto),
                routeDto.enabled()
        };
    }

    /**
     * Recursively maps a RouteDto by deep cloning all its nested structures.
     * Useful for creating independent copies of complex route configurations.
     *
     * @param routeDto the RouteDto to deep clone
     * @return a new RouteDto instance with all nested structures cloned
     */
    public static RouteDto deepCloneRouteDto(RouteDto routeDto) {
        if (routeDto == null) {
            return null;
        }
        // Convert to JSON and back to create a deep clone
        String json = routeDtoToJson(routeDto);
        return jsonToRouteDto(json);
    }

    /**
     * Merges two RouteDtos, with newRoute taking precedence over oldRoute.
     * Non-null values from newRoute override oldRoute values.
     *
     * @param oldRoute the original RouteDto
     * @param newRoute the new RouteDto with updates
     * @return a merged RouteDto
     */
    public static RouteDto mergeRouteDtos(RouteDto oldRoute, RouteDto newRoute) {
        if (newRoute == null) {
            return oldRoute;
        }
        if (oldRoute == null) {
            return newRoute;
        }

        return new RouteDto(
                nvl(newRoute.stripPrefix(), oldRoute.stripPrefix()),
                nvl(newRoute.pathPattern(), oldRoute.pathPattern()),
                newRoute.timeoutMs() > 0 ? newRoute.timeoutMs() : oldRoute.timeoutMs(),
                nvl(newRoute.targets(), oldRoute.targets()),
                newRoute.enabled() != oldRoute.enabled() ? newRoute.enabled() : oldRoute.enabled(),
                nvl(newRoute.headerRules(), oldRoute.headerRules()),
                nvl(newRoute.circuitBreakerPolicy(), oldRoute.circuitBreakerPolicy()),
                nvl(newRoute.loadBalancerType(), oldRoute.loadBalancerType()),
                nvl(newRoute.authForwardHeaders(), oldRoute.authForwardHeaders()),
                nvl(newRoute.rateLimitPolicy(), oldRoute.rateLimitPolicy()),
                nvl(newRoute.routingType(), oldRoute.routingType()),
                nvl(newRoute.cachePolicy(), oldRoute.cachePolicy()),
                nvl(newRoute.name(), oldRoute.name()),
                nvl(newRoute.auditStore(), oldRoute.auditStore()),
                nvl(newRoute.id(), oldRoute.id()),
                newRoute.auditEnabled() != oldRoute.auditEnabled() ? newRoute.auditEnabled() : oldRoute.auditEnabled(),
                nvl(newRoute.metaData(), oldRoute.metaData())
        );
    }

    /**
     * Finds differences between two RouteDtos.
     * Returns a map of properties that have changed.
     *
     * @param oldRoute the original RouteDto
     * @param newRoute the modified RouteDto
     * @return Map of property names to DiffModel objects
     */
    public static java.util.Map<String, DiffModel> findRouteDifferences(RouteDto oldRoute, RouteDto newRoute) {
        return DiffHelper.findDifferences(oldRoute, newRoute);
    }

    private static <T> T nvl(T newValue, T oldValue) {
        return newValue != null ? newValue : oldValue;
    }

    // ── RouteDto ↔ RouteDao (persistence model) ───────────────────────────────

    /**
     * Converts a {@link RouteDto} (API / JSON model) to a
     * {@link org.example.gateway.routes.dao.RouteDao} (persistence model).
     * All nested sub-objects are mapped field-by-field between the two packages.
     */
    public static org.example.gateway.routes.dao.RouteDao routeDtoToRouteDao(RouteDto dto) {
        if (dto == null) return null;

        return new org.example.gateway.routes.dao.RouteDao(
                dto.stripPrefix(),
                dto.pathPattern(),
                dto.timeoutMs(),
                dto.targets() == null ? null : dto.targets().stream()
                        .map(t -> t == null ? null : new org.example.gateway.routes.dao.TargetsItem(
                                t.headerMatchValue(), t.weight(), t.headerMatchName(), t.url()))
                        .toList(),
                dto.enabled(),
                toHeaderRulesDao(dto.headerRules()),
                toCircuitBreakerDao(dto.circuitBreakerPolicy()),
                dto.loadBalancerType(),
                dto.authForwardHeaders(),
                toRateLimitDao(dto.rateLimitPolicy()),
                dto.routingType(),
                toCachePolicyDao(dto.cachePolicy()),
                dto.name(),
                dto.auditStore(),
                dto.id(),
                dto.auditEnabled(),
                dto.metaData());
    }

    /**
     * Converts a {@link org.example.gateway.routes.dao.RouteDao} (persistence model)
     * to a {@link RouteDto} (API / JSON model).
     */
    public static RouteDto routeDaoToRouteDto(org.example.gateway.routes.dao.RouteDao dao) {
        if (dao == null) return null;

        return new RouteDto(
                dao.stripPrefix(),
                dao.pathPattern(),
                dao.timeoutMs(),
                dao.targets() == null ? null : dao.targets().stream()
                        .map(t -> t == null ? null : new org.example.gateway.routes.model.TargetsItem(
                                t.headerMatchValue(), t.weight(), t.headerMatchName(), t.url()))
                        .toList(),
                dao.enabled(),
                toHeaderRulesDto(dao.headerRules()),
                toCircuitBreakerDto(dao.circuitBreakerPolicy()),
                dao.loadBalancerType(),
                dao.authForwardHeaders(),
                toRateLimitDto(dao.rateLimitPolicy()),
                dao.routingType(),
                toCachePolicyDto(dao.cachePolicy()),
                dao.name(),
                dao.auditStore(),
                dao.id(),
                dao.auditEnabled(),
                dao.metaData());
    }

    // ── Private conversion helpers ────────────────────────────────────────────

    private static org.example.gateway.routes.dao.HeaderRules toHeaderRulesDao(
            org.example.gateway.routes.model.HeaderRules src) {
        if (src == null) return null;
        return new org.example.gateway.routes.dao.HeaderRules(
                src.excludeRequest(),
                src.addRequest() == null ? null
                        : src.addRequest().stream()
                                .map(r -> new org.example.gateway.routes.dao.AddRequest(r.name(), r.value()))
                                .toList(),
                src.excludeResponse(),
                src.addResponse() == null ? null
                        : src.addResponse().stream()
                                .map(r -> new org.example.gateway.routes.dao.AddResponse(r.name(), r.value()))
                                .toList());
    }

    private static org.example.gateway.routes.model.HeaderRules toHeaderRulesDto(
            org.example.gateway.routes.dao.HeaderRules src) {
        if (src == null) return null;
        return new org.example.gateway.routes.model.HeaderRules(
                src.excludeRequest(),
                src.addRequest() == null ? null
                        : src.addRequest().stream()
                                .map(r -> new org.example.gateway.routes.model.AddRequest(r.name(), r.value()))
                                .toList(),
                src.excludeResponse(),
                src.addResponse() == null ? null
                        : src.addResponse().stream()
                                .map(r -> new org.example.gateway.routes.model.AddResponse(r.name(), r.value()))
                                .toList());
    }

    private static org.example.gateway.routes.dao.CircuitBreakerPolicy toCircuitBreakerDao(
            org.example.gateway.routes.model.CircuitBreakerPolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.dao.CircuitBreakerPolicy(
                src.waitDurationSeconds(), src.slidingWindowSize(),
                src.failureRateThreshold(), src.enabled());
    }

    private static org.example.gateway.routes.model.CircuitBreakerPolicy toCircuitBreakerDto(
            org.example.gateway.routes.dao.CircuitBreakerPolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.model.CircuitBreakerPolicy(
                src.waitDurationSeconds(), src.slidingWindowSize(),
                src.failureRateThreshold(), src.enabled());
    }

    private static org.example.gateway.routes.dao.RateLimitPolicy toRateLimitDao(
            org.example.gateway.routes.model.RateLimitPolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.dao.RateLimitPolicy(
                src.timeoutDurationMs(), src.burst(), src.requestsPerSecond(), src.enabled());
    }

    private static org.example.gateway.routes.model.RateLimitPolicy toRateLimitDto(
            org.example.gateway.routes.dao.RateLimitPolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.model.RateLimitPolicy(
                src.timeoutDurationMs(), src.burst(), src.requestsPerSecond(), src.enabled());
    }

    private static org.example.gateway.routes.dao.CachePolicy toCachePolicyDao(
            org.example.gateway.routes.model.CachePolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.dao.CachePolicy(
                src.ttlSeconds(), src.cacheKeyStrategy(), src.enabled());
    }

    private static org.example.gateway.routes.model.CachePolicy toCachePolicyDto(
            org.example.gateway.routes.dao.CachePolicy src) {
        if (src == null) return null;
        return new org.example.gateway.routes.model.CachePolicy(
                src.ttlSeconds(), src.cacheKeyStrategy(), src.enabled());
    }
}
