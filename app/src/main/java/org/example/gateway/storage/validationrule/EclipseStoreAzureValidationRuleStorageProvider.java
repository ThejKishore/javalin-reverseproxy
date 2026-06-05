/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.eclipse.store.afs.azure.storage.types.AzureStorageConnector;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.routes.dao.ValidationRuleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * {@link ValidationRuleStorageProvider} backed by EclipseStore on Azure Blob Storage.
 *
 * <p>Uses container {@code <containerName>-validationrule} to isolate rule data.
 */
public class EclipseStoreAzureValidationRuleStorageProvider implements ValidationRuleStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreAzureValidationRuleStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final ValidationRuleStoreRoot root;

    public EclipseStoreAzureValidationRuleStorageProvider(String connectionString, String containerName) {
        String ruleContainer = containerName + "-validationrule";
        log.info("Starting EclipseStore Azure validation-rule storage (container='{}')", ruleContainer);

        var blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        AzureStorageConnector connector = AzureStorageConnector.New(blobServiceClient);
        BlobStoreFileSystem fileSystem = BlobStoreFileSystem.New(connector);

        ValidationRuleStoreRoot initialRoot = new ValidationRuleStoreRoot();
        this.storageManager = EmbeddedStorage.start(initialRoot,
                fileSystem.ensureDirectoryPath(ruleContainer));

        Object stored = storageManager.root();
        this.root = stored instanceof ValidationRuleStoreRoot r ? r : initialRoot;
        log.info("EclipseStore Azure validation-rule: loaded {} entries", root.getEntries().size());
    }

    @Override
    public List<ValidationRuleEntry> findAll() {
        synchronized (root) {
            return List.copyOf(root.getEntries());
        }
    }

    @Override
    public List<ValidationRuleEntry> findAllEnabled() {
        synchronized (root) {
            return root.getEntries().stream()
                    .filter(ValidationRuleEntry::enabled)
                    .toList();
        }
    }

    @Override
    public void insert(String id, String name, String pattern, String target, boolean enabled) {
        ValidationRuleEntry entry = new ValidationRuleEntry(id, name, pattern, target, enabled,
                Instant.now(), Instant.now());
        synchronized (root) {
            root.getEntries().add(entry);
            storageManager.store(root.getEntries());
        }
        log.debug("EclipseStore Azure validation-rule: inserted id={} name={}", id, name);
    }

    @Override
    public int setEnabled(String id, boolean enabled) {
        synchronized (root) {
            List<ValidationRuleEntry> entries = root.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                ValidationRuleEntry existing = entries.get(i);
                if (id.equals(existing.id())) {
                    ValidationRuleEntry updated = new ValidationRuleEntry(
                            existing.id(), existing.name(), existing.pattern(), existing.target(),
                            enabled, existing.createdAt(), Instant.now());
                    entries.set(i, updated);
                    storageManager.store(entries);
                    log.debug("EclipseStore Azure validation-rule: set id={} enabled={}", id, enabled);
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public int deleteById(String id) {
        synchronized (root) {
            boolean removed = root.getEntries().removeIf(e -> id.equals(e.id()));
            if (removed) {
                storageManager.store(root.getEntries());
                log.debug("EclipseStore Azure validation-rule: deleted id={}", id);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void close() {
        storageManager.shutdown();
        log.info("EclipseStore Azure validation-rule storage shut down");
    }
}
