/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.routes.dao.ChangeLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 * {@link ChangeLogStorageProvider} backed by EclipseStore on the local filesystem.
 *
 * <p>Change-log entries are stored as a binary object graph under
 * {@code storagePath/changelog}.  All read filtering is performed in-memory since
 * EclipseStore doesn't have a query engine.
 */
public class EclipseStoreLocalChangeLogStorageProvider implements ChangeLogStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreLocalChangeLogStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final ChangeLogStoreRoot root;

    public EclipseStoreLocalChangeLogStorageProvider(String storagePath) {
        var path = Paths.get(storagePath, "changelog");
        log.info("Starting EclipseStore local changelog storage at '{}'", path.toAbsolutePath());
        ChangeLogStoreRoot initialRoot = new ChangeLogStoreRoot();
        this.storageManager = EmbeddedStorage.start(initialRoot, path);
        Object stored = storageManager.root();
        this.root = stored instanceof ChangeLogStoreRoot r ? r : initialRoot;
        log.info("EclipseStore local changelog: loaded {} entries", root.getEntries().size());
    }

    @Override
    public void insert(ChangeLogEntry entry) {
        synchronized (root) {
            root.getEntries().add(entry);
            storageManager.store(root.getEntries());
        }
        log.debug("EclipseStore local changelog: inserted entry action={}", entry.action());
    }

    @Override
    public List<ChangeLogEntry> findRecent(int limit) {
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
    public List<ChangeLogEntry> findByRouteId(String routeId, int limit) {
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
    public void close() {
        storageManager.shutdown();
        log.info("EclipseStore local changelog storage shut down");
    }
}
