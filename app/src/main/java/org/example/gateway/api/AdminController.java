/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.example.gateway.config.YamlConfigLoader;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.gateway.routes.dao.AuditLogEntry;
import org.example.gateway.routes.dao.ChangeLogEntry;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.mapper.RouteMapper;
import org.example.gateway.routes.model.RouteDto;
import org.example.gateway.security.ValidationRuleStore;
import org.example.gateway.storage.audit.AuditStorageProvider;
import org.example.gateway.storage.changelog.ChangeLogStorageProvider;
import org.example.gateway.storage.routes.RouteStorageProvider;
import org.example.gateway.storage.validationrule.ValidationRuleStorageProvider;
import org.example.utilities.gateway.exception.GatewayException;
import org.example.utilities.gateway.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for dynamic route management exposed under
 * {@code /gateway/admin}.
 *
 * <p>The API layer uses {@link RouteDto} for all request/response bodies.
 * Internally, routes are stored via {@link RouteStorageProvider} (using
 * {@link org.example.gateway.routes.dao.RouteDao}) and the live registry
 * uses {@link RouteDefinition} for proxy dispatch.
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/gateway/admin/routes</td><td>List all routes</td></tr>
 *   <tr><td>GET</td><td>/gateway/admin/routes/{pathPattern}</td><td>Get a route</td></tr>
 *   <tr><td>POST</td><td>/gateway/admin/routes</td><td>Create a route</td></tr>
 *   <tr><td>PUT</td><td>/gateway/admin/routes/{pathPattern}</td><td>Replace a route</td></tr>
 *   <tr><td>DELETE</td><td>/gateway/admin/routes/{pathPattern}</td><td>Delete a route</td></tr>
 *   <tr><td>PATCH</td><td>/gateway/admin/routes/{pathPattern}/enable</td><td>Enable a route</td></tr>
 *   <tr><td>PATCH</td><td>/gateway/admin/routes/{pathPattern}/disable</td><td>Disable a route</td></tr>
 *   <tr><td>POST</td><td>/gateway/admin/reload</td><td>Re-load all routes</td></tr>
 * </table>
 */
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final RouteRegistry registry;
    private final RouteLoader loader;

    private final ObjectMapper jsonMapper = YamlConfigLoader.jsonMapper();
    private final ValidationRuleStore validationRuleStore;
    private final RouteStorageProvider storageProvider;
    private final AuditStorageProvider auditProvider;
    private final ChangeLogStorageProvider changeLogProvider;
    private final ValidationRuleStorageProvider validationRuleStorageProvider;

    /** Legacy constructor — no storage provider (tests / yaml-only mode). */
    public AdminController(RouteRegistry registry, RouteLoader loader) {
        this(registry, loader, null, null, null, null, null);
    }

    /** Constructor with validation-rule store but no storage provider. */
    public AdminController(RouteRegistry registry, RouteLoader loader,
                           ValidationRuleStore validationRuleStore) {
        this(registry, loader, null, validationRuleStore, null, null, null);
    }

    /** Constructor with route storage provider and validation-rule store. */
    public AdminController(RouteRegistry registry, RouteLoader loader,
                           RouteStorageProvider storageProvider,
                           ValidationRuleStore validationRuleStore) {
        this(registry, loader, storageProvider, validationRuleStore, null, null, null);
    }

    /** Full constructor with all storage providers. */
    public AdminController(RouteRegistry registry, RouteLoader loader,
                           RouteStorageProvider storageProvider,
                           ValidationRuleStore validationRuleStore,
                           AuditStorageProvider auditProvider,
                           ChangeLogStorageProvider changeLogProvider,
                           ValidationRuleStorageProvider validationRuleStorageProvider) {
        this.registry = registry;
        this.loader = loader;
        this.storageProvider = storageProvider;
        this.validationRuleStore = validationRuleStore;
        this.auditProvider = auditProvider;
        this.changeLogProvider = changeLogProvider;
        this.validationRuleStorageProvider = validationRuleStorageProvider;
    }

    // ── Route CRUD ────────────────────────────────────────────────────────────

    /// Lists all routes, returned as {@link RouteDto} objects.
    public void listRoutes(Context ctx) {
        List<RouteDto> dtos = storageProvider != null
                ? storageProvider.findAll().stream().map(this::toRouteDto).toList()
                : registry.getAllRoutes().stream().map(route -> withVersion(toRouteDto(route), 0L)).toList();
        ctx.json(dtos);
    }

    /// Returns a single route by path-pattern as a {@link RouteDto}.
    public void getRoute(Context ctx) {
        String pathPattern = routeKey(ctx);
        RouteDto dto = findRouteDto(pathPattern)
                .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
        ctx.json(dto);
    }

    /// Creates a new route from a {@link RouteDto} body.
    public void createRoute(Context ctx) throws Exception {
        RouteDto dto = parseBody(ctx);
        String pathPattern = normalizePathPattern(dto.pathPattern());
        if (pathPattern == null) {
            throw new BadRequestResponse("'path-pattern' is required");
        }
        if (dto.id() == null || dto.id().isBlank()) {
            dto = withId(dto, UUID.randomUUID().toString());
        }
        dto = withPathPattern(dto, pathPattern);
        dto = withVersion(dto, 1L);
        if (findRouteDto(pathPattern).isPresent()) {
            throw new GatewayException("Duplicate route path-pattern: " + pathPattern, 409);
        }
        // Persist via storage provider
        if (storageProvider != null) {
            storageProvider.insert(RouteMapper.routeDtoToRouteDao(dto));
        }
        // Update live registry
        RouteDefinition def = toRouteDefinition(dto);
        registry.addOrUpdate(def);
        writeAuditLog("CREATE_ROUTE", dto.id(), dto.name(), ctx, 201);
        writeChangeLog("CREATE_ROUTE", def.getId(), def.getName(),
                "Created route: " + def.getName() + " [" + def.getPathPattern() + "]", ctx.ip());
        ctx.status(201).json(resolveRouteDto(pathPattern, def, dto.version()));
    }

    /// Replaces an existing route's configuration from a {@link RouteDto} body.
    public void updateRoute(Context ctx) throws Exception {
        String pathPattern = routeKey(ctx);
        RouteDto current = findRouteDto(pathPattern)
                .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
        RouteDto dto = withPathPattern(parseBody(ctx), pathPattern);
        dto = withId(dto, current.id());
        if (dto.version() <= 0) {
            throw new BadRequestResponse("'version' is required for updates");
        }
        // Persist via storage provider
        if (storageProvider != null) {
            int updated = storageProvider.update(RouteMapper.routeDtoToRouteDao(dto));
            if (updated == 0) {
                throw new GatewayException("Route version conflict for path-pattern: " + pathPattern, 409);
            }
            current = storageProvider.findById(pathPattern)
                    .map(this::toRouteDto)
                    .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
            dto = current;
        }
        // Update live registry
        RouteDefinition def = toRouteDefinition(dto);
        registry.addOrUpdate(def);
        writeAuditLog("UPDATE_ROUTE", current.id(), dto.name(), ctx, 200);
        writeChangeLog("UPDATE_ROUTE", def.getId(), def.getName(),
                "Updated route: " + def.getName() + " [" + def.getPathPattern() + "]", ctx.ip());
        ctx.json(dto);
    }

    /// Deletes a route by ID.
    public void deleteRoute(Context ctx) {
        String pathPattern = routeKey(ctx);
        RouteDto current = findRouteDto(pathPattern)
                .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
        String routeName = current.name() != null ? current.name() : pathPattern;
        if (storageProvider != null) {
            storageProvider.deleteById(pathPattern);
        }
        registry.remove(current.id());
        writeAuditLog("DELETE_ROUTE", current.id(), routeName, ctx, 204);
        writeChangeLog("DELETE_ROUTE", current.id(), routeName, "Deleted route: " + routeName, ctx.ip());
        ctx.status(204);
    }

    public void enableRoute(Context ctx) {
        toggleRoute(ctx, true);
    }

    public void disableRoute(Context ctx) {
        toggleRoute(ctx, false);
    }

    public void reload(Context ctx) {
        loader.load();
        writeChangeLog("RELOAD_GATEWAY", null, null,
                "Gateway reloaded — " + registry.getRoutes().size() + " routes active", ctx.ip());
        ctx.json(Map.of("status", "reloaded", "routes", registry.getRoutes().size()));
    }



    // ── Security: Validation rules ────────────────────────────────────────────

    /// Lists all validation rules (enabled and disabled).
    public void listValidationRules(Context ctx) {
        if (validationRuleStorageProvider == null) {
            ctx.json(Map.of("rules", List.of()));
            return;
        }
        var rules = validationRuleStorageProvider.findAll();
        ctx.json(Map.of("rules", rules, "total", rules.size()));
    }

    /// Creates a new validation rule.
    public void createValidationRule(Context ctx) {
        if (validationRuleStorageProvider == null) throw new BadRequestResponse("Validation-rule storage not configured");
        @SuppressWarnings("unchecked")
        var body = ctx.bodyAsClass(Map.class);
        String id      = UUID.randomUUID().toString();
        String name    = (String) body.get("name");
        String pattern = (String) body.get("pattern");
        String target  = body.getOrDefault("target", "ALL").toString();
        boolean enabled = body.containsKey("enabled")
                ? Boolean.parseBoolean(body.get("enabled").toString()) : true;

        if (name == null || pattern == null) throw new BadRequestResponse("'name' and 'pattern' are required");

        validationRuleStorageProvider.insert(id, name, pattern, target, enabled);
        if (validationRuleStore != null) validationRuleStore.reload();
        writeChangeLog("CREATE_VALIDATION_RULE", id, name, "Created rule: " + name, ctx.ip());
        ctx.status(201).json(Map.of("id", id, "name", name, "pattern", pattern, "target", target, "enabled", enabled));
    }

    /// Reloads all enabled validation rules into the in-memory store.
    public void reloadValidationRules(Context ctx) {
        if (validationRuleStore != null) validationRuleStore.reload();
        int count = validationRuleStore != null ? validationRuleStore.getRules().size() : 0;
        writeChangeLog("RELOAD_VALIDATION_RULES", null, null,
                "Reloaded " + count + " validation rule(s)", ctx.ip());
        ctx.json(Map.of("status", "reloaded", "rules", count));
    }

    /// Enables a validation rule by ID.
    public void enableValidationRule(Context ctx) {
        toggleValidationRule(ctx, true);
    }

    /// Disables a validation rule by ID.
    public void disableValidationRule(Context ctx) {
        toggleValidationRule(ctx, false);
    }

    /// Deletes a validation rule by ID.
    public void deleteValidationRule(Context ctx) {
        if (validationRuleStorageProvider == null) throw new BadRequestResponse("Validation-rule storage not configured");
        String id = ctx.pathParam("id");
        validationRuleStorageProvider.deleteById(id);
        if (validationRuleStore != null) validationRuleStore.reload();
        writeChangeLog("DELETE_VALIDATION_RULE", id, null, "Deleted validation rule: " + id, ctx.ip());
        ctx.status(204);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void toggleValidationRule(Context ctx, boolean enabled) {
        if (validationRuleStorageProvider == null) throw new BadRequestResponse("Validation-rule storage not configured");
        String id = ctx.pathParam("id");
        validationRuleStorageProvider.setEnabled(id, enabled);
        if (validationRuleStore != null) validationRuleStore.reload();
        writeChangeLog(enabled ? "ENABLE_VALIDATION_RULE" : "DISABLE_VALIDATION_RULE",
                id, null, (enabled ? "Enabled" : "Disabled") + " validation rule: " + id, ctx.ip());
        ctx.json(Map.of("id", id, "enabled", enabled));
    }

    private void toggleRoute(Context ctx, boolean enabled) {
        String pathPattern = routeKey(ctx);
        RouteDto current = findRouteDto(pathPattern)
                .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
        String routeName = current.name() != null ? current.name() : pathPattern;
        if (storageProvider != null) {
            int updated = storageProvider.setEnabled(pathPattern, current.version(), enabled);
            if (updated == 0) {
                throw new GatewayException("Route version conflict for path-pattern: " + pathPattern, 409);
            }
            current = storageProvider.findById(pathPattern)
                    .map(this::toRouteDto)
                    .orElseThrow(() -> new NotFoundResponse("Route not found: " + pathPattern));
        } else {
            current = withEnabled(current, enabled);
        }
        registry.setEnabled(current.id(), enabled);
        writeAuditLog(enabled ? "ENABLE_ROUTE" : "DISABLE_ROUTE", current.id(), routeName, ctx, 200);
        writeChangeLog(enabled ? "ENABLE_ROUTE" : "DISABLE_ROUTE", current.id(), routeName,
                (enabled ? "Enabled" : "Disabled") + " route: " + routeName, ctx.ip());
        ctx.json(withEnabled(current, enabled));
    }

    private void writeChangeLog(String action, String routeId, String routeName, String details, String performedBy) {
        if (changeLogProvider == null) return;
        try {
            ChangeLogEntry entry = new ChangeLogEntry(
                    UUID.randomUUID().toString(), action, routeId, routeName,
                    details, performedBy, Instant.now());
            changeLogProvider.insert(entry);
            log.info("CHANGE_LOG action={} route={} by={} details={}", action, routeName, performedBy, details);
        } catch (Exception e) {
            log.warn("Failed to write change log: {}", e.getMessage());
        }
    }

    private void writeAuditLog(String action, String routeId, String routeName, Context ctx, int statusCode) {
        if (auditProvider == null) return;
        try {
            AuditLogEntry entry = new AuditLogEntry(
                    UUID.randomUUID().toString(),
                    routeId,
                    routeName,
                    requestId(ctx),
                    traceId(ctx),
                    action,
                    ctx.path(),
                    null,
                    statusCode,
                    null,
                    ctx.ip(),
                    Instant.now());
            auditProvider.insert(entry);
            log.info("AUDIT action={} route={} status={}", action, routeName, statusCode);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    /**
     * Parses the request body as a {@link RouteDto}.
     * Accepts both the kebab-case JSON format (RouteDto / RouteDefinition convention)
     * and any additional fields that may be present are silently ignored.
     */
    private RouteDto parseBody(Context ctx) {
        try {
            return jsonMapper.readValue(ctx.body(), RouteDto.class);
        } catch (Exception e) {
            throw new BadRequestResponse("Invalid route JSON: " + e.getMessage());
        }
    }

    /**
     * Converts a {@link RouteDto} to a {@link RouteDefinition} for the live registry.
     * Uses Jackson round-trip so that all common kebab-case fields are mapped automatically;
     * security policy fields (jwt-policy, csrf-policy, csp-policy) present in the JSON
     * are preserved if the caller includes them.
     */
    private RouteDefinition toRouteDefinition(RouteDto dto) {
        try {
            String json = jsonMapper.writeValueAsString(dto);
            return jsonMapper.readValue(json, RouteDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert RouteDto to RouteDefinition", e);
        }
    }

    /**
     * Converts a {@link RouteDefinition} to a {@link RouteDto} for the API response.
     * Security policy fields that are not part of {@link RouteDto} are silently dropped.
     */
    private RouteDto toRouteDto(RouteDefinition def) {
        try {
            String json = jsonMapper.writeValueAsString(def);
            return jsonMapper.readValue(json, RouteDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert RouteDefinition to RouteDto", e);
        }
    }

    private RouteDto toRouteDto(RouteDao dao) {
        return RouteMapper.routeDaoToRouteDto(dao);
    }

    private RouteDto resolveRouteDto(String pathPattern, RouteDefinition fallback, long version) {
        if (storageProvider == null) {
            return withVersion(withPathPattern(toRouteDto(fallback), pathPattern), version);
        }
        return storageProvider.findById(pathPattern)
                .map(this::toRouteDto)
                .orElse(withVersion(withPathPattern(toRouteDto(fallback), pathPattern), version));
    }

    private java.util.Optional<RouteDto> findRouteDto(String id) {
        if (storageProvider != null) {
            return storageProvider.findById(id).map(this::toRouteDto);
        }
        // use partition key and
        return registry.getAllRoutes().stream()
                .filter(r -> {
                    String candidate = normalizePathPattern(r.getPathPattern());
                    return candidate != null && candidate.equals(id);
                })
                .findFirst()
                .map(r -> withVersion(withPathPattern(toRouteDto(r), id), 0L));
    }

    private static RouteDto withPathPattern(RouteDto dto, String pathPattern) {
        return new RouteDto(
                dto.stripPrefix(), pathPattern, dto.timeoutMs(), dto.targets(),
                dto.enabled(), dto.headerRules(), dto.circuitBreakerPolicy(),
                dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                dto.id(), dto.auditEnabled(), dto.version(), dto.metaData());
    }

    private static RouteDto withId(RouteDto dto, String id) {
        return new RouteDto(
                dto.stripPrefix(), dto.pathPattern(), dto.timeoutMs(), dto.targets(),
                dto.enabled(), dto.headerRules(), dto.circuitBreakerPolicy(),
                dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                id, dto.auditEnabled(), dto.version(), dto.metaData());
    }

    private static RouteDto withEnabled(RouteDto dto, boolean enabled) {
        return new RouteDto(
                dto.stripPrefix(), dto.pathPattern(), dto.timeoutMs(), dto.targets(),
                enabled, dto.headerRules(), dto.circuitBreakerPolicy(),
                dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                dto.id(), dto.auditEnabled(), dto.version(), dto.metaData());
    }

    private static RouteDto withVersion(RouteDto dto, long version) {
        return new RouteDto(
                dto.stripPrefix(), dto.pathPattern(), dto.timeoutMs(), dto.targets(),
                dto.enabled(), dto.headerRules(), dto.circuitBreakerPolicy(),
                dto.loadBalancerType(), dto.authForwardHeaders(), dto.rateLimitPolicy(),
                dto.routingType(), dto.cachePolicy(), dto.name(), dto.auditStore(),
                dto.id(), dto.auditEnabled(), version, dto.metaData());
    }

    private String routeKey(Context ctx) {
        return ctx.pathParam("pathPattern");
    }

    private static String requestId(Context ctx) {
        String requestId = ctx.header("X-Request-Id");
        return (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
    }

    private static String traceId(Context ctx) {
        String traceId = ctx.header("X-Trace-Id");
        return (traceId == null || traceId.isBlank()) ? null : traceId;
    }

    private static String normalizePathPattern(String pathPattern) {
        if (pathPattern == null || pathPattern.isBlank()) {
            return null;
        }
        return pathPattern.startsWith("/") ? pathPattern : "/" + pathPattern;
    }
}
