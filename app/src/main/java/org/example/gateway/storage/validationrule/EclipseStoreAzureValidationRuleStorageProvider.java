/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import org.eclipse.serializer.afs.types.ADirectory;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.EclipseStoreConfigSupport;
import org.example.gateway.config.EclipseStoreStorageSettings;
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

    public EclipseStoreAzureValidationRuleStorageProvider(EclipseStoreConfig config) {
        EclipseStoreStorageSettings settings = EclipseStoreConfigSupport.loadAzureSettings(config, "validationrule");
        String ruleContainer = EclipseStoreConfigSupport.suffixedContainer(settings.getAzureContainer(), "-validationrule");
        log.info("Starting EclipseStore Azure validation-rule storage (container='{}')", ruleContainer);

        BlobStoreFileSystem fileSystem = EclipseStoreConfigSupport.newAzureFileSystem(settings);
        ADirectory directory = fileSystem.ensureDirectoryPath(ruleContainer);

        ValidationRuleStoreRoot initialRoot = new ValidationRuleStoreRoot();
        this.storageManager = EclipseStoreConfigSupport.createAndStartStorageManager(
                EclipseStoreConfigSupport.buildAzureFoundation(settings, directory),
                initialRoot);

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
