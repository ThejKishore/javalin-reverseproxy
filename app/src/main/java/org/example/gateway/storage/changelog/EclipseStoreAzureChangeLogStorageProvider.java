/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.eclipse.serializer.afs.types.ADirectory;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.EclipseStoreConfigSupport;
import org.example.gateway.config.EclipseStoreStorageSettings;
import org.example.gateway.routes.dao.ChangeLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * {@link ChangeLogStorageProvider} backed by EclipseStore on Azure Blob Storage.
 *
 * <p>Uses container {@code <containerName>-changelog} to isolate change-log data.
 */
public class EclipseStoreAzureChangeLogStorageProvider implements ChangeLogStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreAzureChangeLogStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final ChangeLogStoreRoot root;

    public EclipseStoreAzureChangeLogStorageProvider(EclipseStoreConfig config) {
        EclipseStoreStorageSettings settings = EclipseStoreConfigSupport.loadAzureSettings(config, "changelog");
        String changelogContainer = EclipseStoreConfigSupport.suffixedContainer(settings.getAzureContainer(), "-changelog");
        log.info("Starting EclipseStore Azure changelog storage (container='{}')", changelogContainer);

        BlobStoreFileSystem fileSystem = EclipseStoreConfigSupport.newAzureFileSystem(settings);
        ADirectory directory = fileSystem.ensureDirectoryPath(changelogContainer);

        ChangeLogStoreRoot initialRoot = new ChangeLogStoreRoot();
        this.storageManager = EclipseStoreConfigSupport.createAndStartStorageManager(
                EclipseStoreConfigSupport.buildAzureFoundation(settings, directory),
                initialRoot);

        Object stored = storageManager.root();
        this.root = stored instanceof ChangeLogStoreRoot r ? r : initialRoot;
        log.info("EclipseStore Azure changelog: loaded {} entries", root.getEntries().size());
    }


    @Override
    public void insert(ChangeLogEntry entry) {
        synchronized (root) {
            root.getEntries().add(entry);
            storageManager.store(root.getEntries());
        }
        log.debug("EclipseStore Azure changelog: inserted entry action={}", entry.action());
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
        log.info("EclipseStore Azure changelog storage shut down");
    }
}
