/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.eclipse.store.afs.azure.storage.types.AzureStorageConnector;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.routes.dao.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * {@link AuditStorageProvider} backed by EclipseStore on Azure Blob Storage.
 *
 * <p>Uses container {@code <containerName>-audit} to isolate audit data from route data.
 */
public class EclipseStoreAzureAuditStorageProvider implements AuditStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreAzureAuditStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final AuditStoreRoot root;

    public EclipseStoreAzureAuditStorageProvider(String connectionString, String containerName) {
        String auditContainer = containerName + "-audit";
        log.info("Starting EclipseStore Azure audit storage (container='{}')", auditContainer);

        var blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        AzureStorageConnector connector = AzureStorageConnector.New(blobServiceClient);
        BlobStoreFileSystem fileSystem = BlobStoreFileSystem.New(connector);

        AuditStoreRoot initialRoot = new AuditStoreRoot();
        this.storageManager = EmbeddedStorage.start(initialRoot,
                fileSystem.ensureDirectoryPath(auditContainer));

        Object stored = storageManager.root();
        this.root = stored instanceof AuditStoreRoot r ? r : initialRoot;
        log.info("EclipseStore Azure audit: loaded {} entries", root.getEntries().size());
    }

    @Override
    public void insert(AuditLogEntry entry) {
        synchronized (root) {
            root.getEntries().add(entry);
            storageManager.store(root.getEntries());
        }
        log.debug("EclipseStore Azure audit: inserted entry requestId={}", entry.requestId());
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
        log.info("EclipseStore Azure audit storage shut down");
    }
}
