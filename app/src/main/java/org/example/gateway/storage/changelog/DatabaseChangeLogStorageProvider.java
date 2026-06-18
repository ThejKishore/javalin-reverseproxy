/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import org.example.gateway.routes.dao.ChangeLogEntry;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@link ChangeLogStorageProvider} backed by JDBI (H2 / PostgreSQL).
 *
 * <p>Converts between the JDBI {@link ChangeLogRow} bean and the shared
 * {@link ChangeLogEntry} record on every read/write.
 */
public class DatabaseChangeLogStorageProvider implements ChangeLogStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseChangeLogStorageProvider.class);

    private final Jdbi jdbi;

    public DatabaseChangeLogStorageProvider(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void insert(ChangeLogEntry entry) {
        ChangeLogRow row = toRow(entry);
        jdbi.useExtension(ChangeLogDao.class, dao -> dao.insert(row));
        log.debug("DB changelog: inserted entry action={} routeId={}", entry.action(), entry.routeId());
    }

    @Override
    public List<ChangeLogEntry> findRecent(int limit) {
        return jdbi.withExtension(ChangeLogDao.class, dao ->
                dao.findRecent(limit).stream().map(this::toEntry).toList());
    }

    @Override
    public List<ChangeLogEntry> findByRouteId(String routeId, int limit) {
        return jdbi.withExtension(ChangeLogDao.class, dao ->
                dao.findByRouteId(routeId, limit).stream().map(this::toEntry).toList());
    }

    // ── Converters ────────────────────────────────────────────────────────────

    private ChangeLogRow toRow(ChangeLogEntry e) {
        ChangeLogRow row = new ChangeLogRow();
        row.setId(e.id());
        row.setAction(e.action());
        row.setRouteId(e.routeId());
        row.setRouteName(e.routeName());
        row.setDetails(e.details());
        row.setPerformedBy(e.performedBy());
        row.setCreatedAt(e.createdAt());
        return row;
    }

    private ChangeLogEntry toEntry(ChangeLogRow row) {
        return new ChangeLogEntry(
                row.getId(), row.getAction(), row.getRouteId(),
                row.getRouteName(), row.getDetails(),
                row.getPerformedBy(), row.getCreatedAt());
    }
}
