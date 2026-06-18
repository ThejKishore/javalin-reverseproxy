/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import org.example.gateway.routes.dao.ChangeLogEntry;
import org.example.gateway.storage.azuretable.AzureTableClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link ChangeLogStorageProvider} backed by Azure Table Storage.
 *
 * <p>Entity mapping:
 * <ul>
 *   <li>PartitionKey = {@code routeId} — enables efficient per-route queries.</li>
 *   <li>RowKey       = entry {@code id} (UUID).</li>
 *   <li>Properties   = all {@link ChangeLogEntry} fields.</li>
 * </ul>
 *
 * <p>Activated when {@code gateway.config-source = azure-table}.
 */
public class AzureTableChangeLogStorageProvider implements ChangeLogStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureTableChangeLogStorageProvider.class);

    private static final String COL_ID           = "id";
    private static final String COL_ACTION       = "action";
    private static final String COL_ROUTE_ID     = "routeId";
    private static final String COL_ROUTE_NAME   = "routeName";
    private static final String COL_DETAILS      = "details";
    private static final String COL_PERFORMED_BY = "performedBy";
    private static final String COL_CREATED_AT   = "createdAt";

    private final TableClient tableClient;

    public AzureTableChangeLogStorageProvider(AzureTableClientFactory factory) {
        this.tableClient = factory.changelogClient();
        log.info("AzureTableChangeLogStorageProvider initialised (table={})", tableClient.getTableName());
    }

    @Override
    public void insert(ChangeLogEntry entry) {
        TableEntity entity = new TableEntity(entry.routeId(), entry.id());
        entity.addProperty(COL_ID, entry.id());
        entity.addProperty(COL_ACTION, entry.action());
        entity.addProperty(COL_ROUTE_ID, entry.routeId());
        entity.addProperty(COL_ROUTE_NAME, entry.routeName());
        entity.addProperty(COL_DETAILS, entry.details());
        entity.addProperty(COL_PERFORMED_BY, entry.performedBy());
        entity.addProperty(COL_CREATED_AT, entry.createdAt() != null ? entry.createdAt().toString() : null);
        tableClient.createEntity(entity);
        log.debug("Azure Table changelog: inserted action={} routeId={}", entry.action(), entry.routeId());
    }

    @Override
    public List<ChangeLogEntry> findRecent(int limit) {
        List<ChangeLogEntry> all = new ArrayList<>();
        tableClient.listEntities().forEach(entity -> all.add(toEntry(entity)));
        all.sort(Comparator.comparing(ChangeLogEntry::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return all.subList(0, Math.min(limit, all.size()));
    }

    @Override
    public List<ChangeLogEntry> findByRouteId(String routeId, int limit) {
        String filter = String.format("PartitionKey eq '%s'", escape(routeId));
        ListEntitiesOptions options = new ListEntitiesOptions().setFilter(filter);
        List<ChangeLogEntry> results = new ArrayList<>();
        tableClient.listEntities(options, null, null).forEach(entity -> results.add(toEntry(entity)));
        results.sort(Comparator.comparing(ChangeLogEntry::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return results.subList(0, Math.min(limit, results.size()));
    }

    // ── Converter ─────────────────────────────────────────────────────────────

    private ChangeLogEntry toEntry(TableEntity entity) {
        return new ChangeLogEntry(
                str(entity, COL_ID),
                str(entity, COL_ACTION),
                str(entity, COL_ROUTE_ID),
                str(entity, COL_ROUTE_NAME),
                str(entity, COL_DETAILS),
                str(entity, COL_PERFORMED_BY),
                instantProp(entity, COL_CREATED_AT));
    }

    // ── Property helpers ──────────────────────────────────────────────────────

    private static String str(TableEntity e, String key) {
        Object v = e.getProperty(key);
        return v != null ? v.toString() : null;
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
