/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.api;

import io.javalin.http.Context;
import org.example.gateway.storage.audit.AuditStorageProvider;
import org.example.gateway.storage.changelog.ChangeLogStorageProvider;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing audit-log and change-log read endpoints under
 * {@code /gateway/admin}.
 *
 * <p>Both storage providers are optional — if null (e.g. yaml-only mode),
 * the endpoints return empty results rather than failing.
 */
public class AuditController {

    private final AuditStorageProvider auditProvider;
    private final ChangeLogStorageProvider changeLogProvider;

    /** Full constructor with explicit storage providers. */
    public AuditController(AuditStorageProvider auditProvider,
                           ChangeLogStorageProvider changeLogProvider) {
        this.auditProvider = auditProvider;
        this.changeLogProvider = changeLogProvider;
    }

    // ── Audit / change-log endpoints ──────────────────────────────────────────

    public void getAuditLogs(Context ctx) {
        if (auditProvider == null) {
            ctx.json(Map.of("logs", List.of(), "total", 0));
            return;
        }
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);
        var logs = auditProvider.findRecent(limit);
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }

    public void getRouteAuditLogs(Context ctx) {
        if (auditProvider == null) {
            ctx.json(Map.of("logs", List.of(), "total", 0));
            return;
        }
        String routeId = ctx.pathParam("routeId");
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50);
        var logs = auditProvider.findByRouteId(routeId, limit);
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }

    public void getChangeLogs(Context ctx) {
        if (changeLogProvider == null) {
            ctx.json(Map.of("logs", List.of(), "total", 0));
            return;
        }
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);
        var logs = changeLogProvider.findRecent(limit);
        ctx.json(Map.of("logs", logs, "total", logs.size()));
    }
}
