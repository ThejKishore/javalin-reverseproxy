/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.routes.dao.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 * {@link AuditStorageProvider} backed by EclipseStore on the local filesystem.
 *
 * <p>Audit-log entries are stored as a binary object graph under
 * {@code storagePath/audit}.  All read filtering is performed in-memory since
 * EclipseStore doesn't have a query engine.
 */
public class EclipseStoreLocalAuditStorageProvider implements AuditStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreLocalAuditStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final AuditStoreRoot root;

    public EclipseStoreLocalAuditStorageProvider(String storagePath) {
        var path = Paths.get(storagePath, "audit");
        log.info("Starting EclipseStore local audit storage at '{}'", path.toAbsolutePath());
        AuditStoreRoot initialRoot = new AuditStoreRoot();
        this.storageManager = EmbeddedStorage.start(initialRoot, path);
        Object stored = storageManager.root();
        this.root = stored instanceof AuditStoreRoot r ? r : initialRoot;
        log.info("EclipseStore local audit: loaded {} entries", root.getEntries().size());
    }

    @Override
    public void insert(AuditLogEntry entry) {
        synchronized (root) {
            root.getEntries().add(entry);
            storageManager.store(root.getEntries());
        }
        log.debug("EclipseStore local audit: inserted entry requestId={}", entry.requestId());
    }

    @Override
    public List<AuditLogEntry> findRecent(int limit) {
        synchronized (root) {
            return root.getEntries().stream()
                    .sorted(Comparator.comparing(
                            e -> e.createdAt() != null ? e.createdAt() : java.time.Instant.EPOCH,
                            Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        }
    }

    @Override
    public List<AuditLogEntry> findByRouteId(String routeId, int limit) {
        synchronized (root) {
            return root.getEntries().stream()
                    .filter(e -> routeId.equals(e.routeId()))
                    .sorted(Comparator.comparing(
                            e -> e.createdAt() != null ? e.createdAt() : java.time.Instant.EPOCH,
                            Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        }
    }

    @Override
    public List<AuditLogEntry> findByRequestId(String requestId) {
        synchronized (root) {
            return root.getEntries().stream()
                    .filter(e -> requestId.equals(e.requestId()))
                    .toList();
        }
    }

    @Override
    public void close() {
        storageManager.shutdown();
        log.info("EclipseStore local audit storage shut down");
    }
}
