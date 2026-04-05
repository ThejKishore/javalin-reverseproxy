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
import org.example.gateway.db.AuditDao;
import org.example.gateway.db.ChangeLogDao;
import org.example.gateway.db.ChangeLogRow;
import org.example.gateway.db.RouteDao;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for dynamic route management exposed under
 * {@code /gateway/admin}.
 *
 * <table border="1">
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/gateway/admin/routes</td><td>List all routes</td></tr>
 *   <tr><td>GET</td><td>/gateway/admin/routes/{id}</td><td>Get a route</td></tr>
 *   <tr><td>POST</td><td>/gateway/admin/routes</td><td>Create a route</td></tr>
 *   <tr><td>PUT</td><td>/gateway/admin/routes/{id}</td><td>Replace a route</td></tr>
 *   <tr><td>DELETE</td><td>/gateway/admin/routes/{id}</td><td>Delete a route</td></tr>
 *   <tr><td>PATCH</td><td>/gateway/admin/routes/{id}/enable</td><td>Enable a route</td></tr>
 *   <tr><td>PATCH</td><td>/gateway/admin/routes/{id}/disable</td><td>Disable a route</td></tr>
 *   <tr><td>POST</td><td>/gateway/admin/reload</td><td>Re-load all routes</td></tr>
 * </table>
 */
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final RouteRegistry registry;
    private final RouteLoader loader;
    private final Jdbi jdbi;
    private final ObjectMapper jsonMapper = YamlConfigLoader.jsonMapper();

    public AdminController(RouteRegistry registry, RouteLoader loader, Jdbi jdbi) {
        this.registry = registry;
        this.loader = loader;
        this.jdbi = jdbi;
    }

    public void listRoutes(Context ctx) {
        ctx.json(registry.getAllRoutes());
    }

    public void getRoute(Context ctx) {
        String id = ctx.pathParam("id");
        RouteDefinition route = registry.getAllRoutes().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundResponse("Route not found: " + id));
        ctx.json(route);
    }

    public void createRoute(Context ctx) throws Exception {
        RouteDefinition route = parseBody(ctx);
        if (route.getId() == null || route.getId().isBlank()) {
            route.setId(java.util.UUID.randomUUID().toString());
        }
        if (jdbi != null) {
            String json = jsonMapper.writeValueAsString(route);
            jdbi.useExtension(RouteDao.class, dao ->
                    dao.insert(route.getId(), route.getName(), json, route.isEnabled()));
        }
        registry.addOrUpdate(route);
        writeChangeLog("CREATE_ROUTE", route.getId(), route.getName(),
                "Created route: " + route.getName() + " [" + route.getPathPattern() + "]", ctx.ip());
        ctx.status(201).json(route);
    }

    public void updateRoute(Context ctx) throws Exception {
        String id = ctx.pathParam("id");
        // Verify the route exists (in any state) before accepting the update
        boolean exists = registry.getAllRoutes().stream().anyMatch(r -> r.getId().equals(id));
        if (!exists && jdbi == null) {
            throw new NotFoundResponse("Route not found: " + id);
        }
        RouteDefinition route = parseBody(ctx);
        route.setId(id);
        if (jdbi != null) {
            String json = jsonMapper.writeValueAsString(route);
            int updated = jdbi.withExtension(RouteDao.class, dao ->
                    dao.update(id, route.getName(), json, route.isEnabled()));
            if (updated == 0 && !exists) throw new NotFoundResponse("Route not found: " + id);
        }
        registry.addOrUpdate(route);
        writeChangeLog("UPDATE_ROUTE", route.getId(), route.getName(),
                "Updated route: " + route.getName() + " [" + route.getPathPattern() + "]", ctx.ip());
        ctx.json(route);
    }

    public void deleteRoute(Context ctx) {
        String id = ctx.pathParam("id");
        // Capture name before removing
        String routeName = registry.getAllRoutes().stream()
                .filter(r -> r.getId().equals(id))
                .map(RouteDefinition::getName)
                .findFirst().orElse(id);
        if (jdbi != null) {
            jdbi.useExtension(RouteDao.class, dao -> dao.deleteById(id));
        }
        registry.remove(id);
        writeChangeLog("DELETE_ROUTE", id, routeName, "Deleted route: " + routeName, ctx.ip());
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

    public void getAuditLogs(Context ctx) {
        if (jdbi == null) {
            ctx.json(Map.of("logs", new java.util.ArrayList<>(), "total", 0));
            return;
        }
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);
        var logs = jdbi.withExtension(AuditDao.class, dao -> dao.findRecent(limit));
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }

    public void getRouteAuditLogs(Context ctx) {
        if (jdbi == null) {
            ctx.json(Map.of("logs", new java.util.ArrayList<>(), "total", 0));
            return;
        }
        String routeId = ctx.pathParam("routeId");
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
        var logs = jdbi.withExtension(AuditDao.class, dao -> dao.findByRouteId(routeId, limit));
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }

    public void getChangeLogs(Context ctx) {
        if (jdbi == null) {
            ctx.json(Map.of("logs", new java.util.ArrayList<>(), "total", 0));
            return;
        }
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);
        var logs = jdbi.withExtension(ChangeLogDao.class, dao -> dao.findRecent(limit));
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void toggleRoute(Context ctx, boolean enabled) {
        String id = ctx.pathParam("id");
        String routeName = registry.getAllRoutes().stream()
                .filter(r -> r.getId().equals(id))
                .map(RouteDefinition::getName)
                .findFirst().orElse(id);
        if (jdbi != null) {
            jdbi.useExtension(RouteDao.class, dao -> dao.setEnabled(id, enabled));
        }
        registry.setEnabled(id, enabled);
        writeChangeLog(enabled ? "ENABLE_ROUTE" : "DISABLE_ROUTE", id, routeName,
                (enabled ? "Enabled" : "Disabled") + " route: " + routeName, ctx.ip());
        ctx.json(Map.of("id", id, "enabled", enabled));
    }

    private void writeChangeLog(String action, String routeId, String routeName, String details, String performedBy) {
        if (jdbi == null) return;
        try {
            ChangeLogRow row = new ChangeLogRow();
            row.setId(UUID.randomUUID().toString());
            row.setAction(action);
            row.setRouteId(routeId);
            row.setRouteName(routeName);
            row.setDetails(details);
            row.setPerformedBy(performedBy);
            jdbi.useExtension(ChangeLogDao.class, dao -> dao.insert(row));
            log.info("CHANGE_LOG action={} route={} by={} details={}", action, routeName, performedBy, details);
        } catch (Exception e) {
            log.warn("Failed to write change log: {}", e.getMessage());
        }
    }

    private RouteDefinition parseBody(Context ctx) {
        try {
            return jsonMapper.readValue(ctx.body(), RouteDefinition.class);
        } catch (Exception e) {
            throw new BadRequestResponse("Invalid route JSON: " + e.getMessage());
        }
    }
}

