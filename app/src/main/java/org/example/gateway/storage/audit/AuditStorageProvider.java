/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import org.example.gateway.routes.dao.AuditLogEntry;

import java.util.List;

/**
 * Persistence contract for gateway audit-log storage.
 *
 * <p>Implementations are selected at startup based on {@code config-source}:
 * <ul>
 *   <li>{@code database}    → {@link DatabaseAuditStorageProvider} (JDBI)</li>
 *   <li>{@code azure-table} → Azure Table Storage implementation</li>
 * </ul>
 *
 * <p>Audit logs are append-only — there are no update or delete operations.
 */
public interface AuditStorageProvider extends AutoCloseable {

    /** Appends a new audit-log entry. */
    void insert(AuditLogEntry entry);

    /** Returns the {@code limit} most recent audit-log entries, newest first. */
    List<AuditLogEntry> findRecent(int limit);

    /**
     * Returns the {@code limit} most recent audit-log entries for a specific route,
     * newest first.
     */
    List<AuditLogEntry> findByRouteId(String routeId, int limit);

    /**
     * Returns all audit-log entries for a specific HTTP request
     * (identified by the correlation / request ID).
     */
    List<AuditLogEntry> findByRequestId(String requestId);

    /** Releases any resources held by this provider. */
    @Override
    default void close() {}
}
