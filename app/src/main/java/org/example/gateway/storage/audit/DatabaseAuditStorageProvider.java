/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import org.example.gateway.routes.dao.AuditLogEntry;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@link AuditStorageProvider} backed by JDBI (H2 / PostgreSQL).
 *
 * <p>Converts between the JDBI {@link AuditLogRow} bean and the shared
 * {@link AuditLogEntry} record on every read/write.
 */
public class DatabaseAuditStorageProvider implements AuditStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuditStorageProvider.class);

    private final Jdbi jdbi;

    public DatabaseAuditStorageProvider(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void insert(AuditLogEntry entry) {
        AuditLogRow row = toRow(entry);
        jdbi.useExtension(AuditDao.class, dao -> dao.insert(row));
        log.debug("DB audit: inserted entry requestId={}", entry.requestId());
    }

    @Override
    public List<AuditLogEntry> findRecent(int limit) {
        return jdbi.withExtension(AuditDao.class, dao ->
                dao.findRecent(limit).stream().map(this::toEntry).toList());
    }

    @Override
    public List<AuditLogEntry> findByRouteId(String routeId, int limit) {
        return jdbi.withExtension(AuditDao.class, dao ->
                dao.findByRouteId(routeId, limit).stream().map(this::toEntry).toList());
    }

    @Override
    public List<AuditLogEntry> findByRequestId(String requestId) {
        return jdbi.withExtension(AuditDao.class, dao ->
                dao.findByRequestId(requestId).stream().map(this::toEntry).toList());
    }

    // ── Converters ────────────────────────────────────────────────────────────

    private AuditLogRow toRow(AuditLogEntry e) {
        AuditLogRow row = new AuditLogRow();
        row.setId(e.id());
        row.setRouteId(e.routeId());
        row.setRouteName(e.routeName());
        row.setRequestId(e.requestId());
        row.setTraceId(e.traceId());
        row.setHttpMethod(e.httpMethod());
        row.setRequestPath(e.requestPath());
        row.setUpstreamUrl(e.upstreamUrl());
        row.setStatusCode(e.statusCode());
        row.setDurationMs(e.durationMs());
        row.setClientIp(e.clientIp());
        row.setCreatedAt(e.createdAt());
        return row;
    }

    private AuditLogEntry toEntry(AuditLogRow row) {
        return new AuditLogEntry(
                row.getId(), row.getRouteId(), row.getRouteName(),
                row.getRequestId(), row.getTraceId(), row.getHttpMethod(),
                row.getRequestPath(), row.getUpstreamUrl(),
                row.getStatusCode(), row.getDurationMs(),
                row.getClientIp(), row.getCreatedAt());
    }
}
