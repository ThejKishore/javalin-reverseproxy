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
import org.example.gateway.db.RouteDao;
import org.example.gateway.db.RouteRow;
import org.example.gateway.registry.RouteLoader;
import org.example.gateway.registry.RouteRegistry;
import org.example.utilities.gateway.model.RouteDefinition;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;

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
        ctx.json(route);
    }

    public void deleteRoute(Context ctx) {
        String id = ctx.pathParam("id");
        if (jdbi != null) {
            jdbi.useExtension(RouteDao.class, dao -> dao.deleteById(id));
        }
        registry.remove(id);
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
        ctx.json(Map.of("status", "reloaded", "routes", registry.getRoutes().size()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void toggleRoute(Context ctx, boolean enabled) {
        String id = ctx.pathParam("id");
        if (jdbi != null) {
            jdbi.useExtension(RouteDao.class, dao -> dao.setEnabled(id, enabled));
        }
        registry.setEnabled(id, enabled);
        ctx.json(Map.of("id", id, "enabled", enabled));
    }

    private RouteDefinition parseBody(Context ctx) {
        try {
            return jsonMapper.readValue(ctx.body(), RouteDefinition.class);
        } catch (Exception e) {
            throw new BadRequestResponse("Invalid route JSON: " + e.getMessage());
        }
    }
}

