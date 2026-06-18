/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import org.example.gateway.routes.dao.ChangeLogEntry;

import java.util.List;

/**
 * Persistence contract for gateway change-log storage.
 *
 * <p>Implementations are selected at startup based on {@code config-source}:
 * <ul>
 *   <li>{@code database}    → {@link DatabaseChangeLogStorageProvider} (JDBI)</li>
 *   <li>{@code azure-table} → Azure Table Storage implementation</li>
 * </ul>
 *
 * <p>Change logs are append-only — there are no update or delete operations.
 */
public interface ChangeLogStorageProvider extends AutoCloseable {

    /** Appends a new change-log entry. */
    void insert(ChangeLogEntry entry);

    /** Returns the {@code limit} most recent change-log entries, newest first. */
    List<ChangeLogEntry> findRecent(int limit);

    /**
     * Returns the {@code limit} most recent change-log entries for a specific route,
     * newest first.
     */
    List<ChangeLogEntry> findByRouteId(String routeId, int limit);

    /** Releases any resources held by this provider. */
    @Override
    default void close() {}
}
