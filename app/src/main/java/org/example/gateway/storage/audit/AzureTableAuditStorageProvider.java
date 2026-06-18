/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import org.example.gateway.routes.dao.AuditLogEntry;
import org.example.gateway.storage.azuretable.AzureTableClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link AuditStorageProvider} backed by Azure Table Storage.
 *
 * <p>Entity mapping:
 * <ul>
 *   <li>PartitionKey = {@code routeId} — enables efficient per-route queries.</li>
 *   <li>RowKey       = entry {@code id} (UUID).</li>
 *   <li>Properties   = all {@link AuditLogEntry} fields.</li>
 * </ul>
 *
 * <p>Activated when {@code gateway.config-source = azure-table}.
 */
public class AzureTableAuditStorageProvider implements AuditStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureTableAuditStorageProvider.class);

    // Table column names
    private static final String COL_ID            = "id";
    private static final String COL_ROUTE_ID      = "routeId";
    private static final String COL_ROUTE_NAME    = "routeName";
    private static final String COL_REQUEST_ID    = "requestId";
    private static final String COL_TRACE_ID      = "traceId";
    private static final String COL_HTTP_METHOD   = "httpMethod";
    private static final String COL_REQUEST_PATH  = "requestPath";
    private static final String COL_UPSTREAM_URL  = "upstreamUrl";
    private static final String COL_STATUS_CODE   = "statusCode";
    private static final String COL_DURATION_MS   = "durationMs";
    private static final String COL_CLIENT_IP     = "clientIp";
    private static final String COL_CREATED_AT    = "createdAt";

    private final TableClient tableClient;

    public AzureTableAuditStorageProvider(AzureTableClientFactory factory) {
        this.tableClient = factory.auditClient();
        log.info("AzureTableAuditStorageProvider initialised (table={})", tableClient.getTableName());
    }

    @Override
    public void insert(AuditLogEntry entry) {
        TableEntity entity = new TableEntity(entry.routeId(), entry.id());
        entity.addProperty(COL_ID, entry.id());
        entity.addProperty(COL_ROUTE_ID, entry.routeId());
        entity.addProperty(COL_ROUTE_NAME, entry.routeName());
        entity.addProperty(COL_REQUEST_ID, entry.requestId());
        entity.addProperty(COL_TRACE_ID, entry.traceId());
        entity.addProperty(COL_HTTP_METHOD, entry.httpMethod());
        entity.addProperty(COL_REQUEST_PATH, entry.requestPath());
        entity.addProperty(COL_UPSTREAM_URL, entry.upstreamUrl());
        entity.addProperty(COL_STATUS_CODE, entry.statusCode());
        entity.addProperty(COL_DURATION_MS, entry.durationMs());
        entity.addProperty(COL_CLIENT_IP, entry.clientIp());
        entity.addProperty(COL_CREATED_AT, entry.createdAt() != null ? entry.createdAt().toString() : null);
        tableClient.createEntity(entity);
        log.debug("Azure Table audit: inserted requestId={}", entry.requestId());
    }

    @Override
    public List<AuditLogEntry> findRecent(int limit) {
        List<AuditLogEntry> all = new ArrayList<>();
        tableClient.listEntities().forEach(entity -> all.add(toEntry(entity)));
        all.sort(Comparator.comparing(AuditLogEntry::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return all.subList(0, Math.min(limit, all.size()));
    }

    @Override
    public List<AuditLogEntry> findByRouteId(String routeId, int limit) {
        String filter = String.format("PartitionKey eq '%s'", escape(routeId));
        ListEntitiesOptions options = new ListEntitiesOptions().setFilter(filter);
        List<AuditLogEntry> results = new ArrayList<>();
        tableClient.listEntities(options, null, null).forEach(entity -> results.add(toEntry(entity)));
        results.sort(Comparator.comparing(AuditLogEntry::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return results.subList(0, Math.min(limit, results.size()));
    }

    @Override
    public List<AuditLogEntry> findByRequestId(String requestId) {
        String filter = String.format("%s eq '%s'", COL_REQUEST_ID, escape(requestId));
        ListEntitiesOptions options = new ListEntitiesOptions().setFilter(filter);
        List<AuditLogEntry> results = new ArrayList<>();
        tableClient.listEntities(options, null, null).forEach(entity -> results.add(toEntry(entity)));
        return results;
    }

    // ── Converter ─────────────────────────────────────────────────────────────

    private AuditLogEntry toEntry(TableEntity entity) {
        return new AuditLogEntry(
                str(entity, COL_ID),
                str(entity, COL_ROUTE_ID),
                str(entity, COL_ROUTE_NAME),
                str(entity, COL_REQUEST_ID),
                str(entity, COL_TRACE_ID),
                str(entity, COL_HTTP_METHOD),
                str(entity, COL_REQUEST_PATH),
                str(entity, COL_UPSTREAM_URL),
                intProp(entity, COL_STATUS_CODE),
                longProp(entity, COL_DURATION_MS),
                str(entity, COL_CLIENT_IP),
                instantProp(entity, COL_CREATED_AT));
    }

    // ── Property helpers ──────────────────────────────────────────────────────

    private static String str(TableEntity e, String key) {
        Object v = e.getProperty(key);
        return v != null ? v.toString() : null;
    }

    private static Integer intProp(TableEntity e, String key) {
        Object v = e.getProperty(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        return Integer.parseInt(v.toString());
    }

    private static Long longProp(TableEntity e, String key) {
        Object v = e.getProperty(key);
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        return Long.parseLong(v.toString());
    }

    private static Instant instantProp(TableEntity e, String key) {
        Object v = e.getProperty(key);
        if (v == null) return null;
        if (v instanceof Instant i) return i;
        try { return Instant.parse(v.toString()); } catch (Exception ex) { return null; }
    }

    /** Escapes single quotes in OData filter strings. */
    private static String escape(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
