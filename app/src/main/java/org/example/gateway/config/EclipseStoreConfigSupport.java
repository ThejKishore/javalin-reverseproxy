/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.eclipse.serializer.afs.types.ADirectory;
import org.eclipse.store.afs.azure.storage.types.AzureStorageConnector;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.afs.nio.types.NioFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.eclipse.store.storage.types.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;


/**
 * Shared fallback helpers for EclipseStore configuration values.
 *
 * <p>This is an internal wiring helper, not part of the external configuration model.
 */
public final class EclipseStoreConfigSupport {

    public static final String DEFAULT_STORAGE_DIRECTORY = "./eclipse-store-data";
    public static final String DEFAULT_AZURE_CONTAINER = "gateway-routes";
    private static final String DEFAULT_ROUTE_LOCAL_CONFIG = "eclipsestore/routes-storage-local.yaml";
    private static final String DEFAULT_ROUTE_AZURE_CONFIG = "eclipsestore/routes-storage-azure.yaml";
    private static final String DEFAULT_AUDIT_LOCAL_CONFIG = "eclipsestore/audit-storage-local.yaml";
    private static final String DEFAULT_AUDIT_AZURE_CONFIG = "eclipsestore/audit-storage-azure.yaml";
    private static final String DEFAULT_CHANGELOG_LOCAL_CONFIG = "eclipsestore/changelog-storage-local.yaml";
    private static final String DEFAULT_CHANGELOG_AZURE_CONFIG = "eclipsestore/changelog-storage-azure.yaml";
    private static final String DEFAULT_VALIDATIONRULE_LOCAL_CONFIG = "eclipsestore/validationrule-storage-local.yaml";
    private static final String DEFAULT_VALIDATIONRULE_AZURE_CONFIG = "eclipsestore/validationrule-storage-azure.yaml";

    private EclipseStoreConfigSupport() {
    }

    public static String storageDirectory(EclipseStoreConfig config) {
        return fallback(config != null ? config.getStorageDirectory() : null, DEFAULT_STORAGE_DIRECTORY);
    }

    public static String azureContainer(EclipseStoreConfig config) {
        return fallback(config != null ? config.getAzureContainer() : null, DEFAULT_AZURE_CONTAINER);
    }

    public static String localConfigPath(EclipseStoreConfig config, String storeName) {
        EclipseStoreStoreConfigPaths paths = storePaths(config, storeName);
        return fallback(paths != null ? paths.getLocalConfig() : null, defaultLocalConfig(storeName));
    }

    public static String azureConfigPath(EclipseStoreConfig config, String storeName) {
        EclipseStoreStoreConfigPaths paths = storePaths(config, storeName);
        return fallback(paths != null ? paths.getAzureConfig() : null, defaultAzureConfig(storeName));
    }

    public static EclipseStoreStorageSettings loadLocalSettings(EclipseStoreConfig config, String storeName) {
        return EclipseStoreStorageConfigurationLoader.load(localConfigPath(config, storeName));
    }

    public static EclipseStoreStorageSettings loadAzureSettings(EclipseStoreConfig config, String storeName) {
        return EclipseStoreStorageConfigurationLoader.load(azureConfigPath(config, storeName));
    }

    public static StorageConfiguration buildLocalStorageConfiguration(EclipseStoreStorageSettings settings) {
        normalizeConfiguredSuffixes(settings);
        String storageDirectory = fallback(settings.getStorageDirectory(), DEFAULT_STORAGE_DIRECTORY);
        repairDoubleDotSuffixFiles(Path.of(storageDirectory), settings);
        NioFileSystem fileSystem = NioFileSystem.New();
        ADirectory directory = fileSystem.ensureDirectoryPath(storageDirectory);
        ADirectory deletionDirectory = optionalDirectory(fileSystem, settings.getDeletionDirectory());
        ADirectory truncationDirectory = optionalDirectory(fileSystem, settings.getTruncationDirectory());
        StorageBackupSetup backupSetup = optionalBackupSetup(fileSystem, settings.getBackupDirectory());

        var fileProviderBuilder = Storage.FileProviderBuilder(fileSystem)
                .setDirectory(directory)
                .setFileNameProvider(buildFileNameProvider(settings));
        if (deletionDirectory != null) {
            fileProviderBuilder.setDeletionDirectory(deletionDirectory);
        }
        if (truncationDirectory != null) {
            fileProviderBuilder.setTruncationDirectory(truncationDirectory);
        }

        StorageConfiguration.Builder<?> builder = Storage.ConfigurationBuilder()
                .setStorageFileProvider(fileProviderBuilder.createFileProvider())
                .setChannelCountProvider(StorageChannelCountProvider.New(
                        settings.getChannelCount() != null ? settings.getChannelCount() : StorageChannelCountProvider.Defaults.defaultChannelCount()))
                .setHousekeepingController(buildHousekeepingController(settings))
                .setEntityCacheEvaluator(buildEntityCacheEvaluator(settings))
                .setDataFileEvaluator(buildDataFileEvaluator(settings));

        if (backupSetup != null) {
            builder.setBackupSetup(backupSetup);
        }

        return builder.createConfiguration();
    }

    public static BlobStoreFileSystem newAzureFileSystem(EclipseStoreStorageSettings settings) {
        var blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(settings.getAzureConnectionString())
                .buildClient();
        AzureStorageConnector connector = AzureStorageConnector.New(blobServiceClient);
        return BlobStoreFileSystem.New(connector);
    }

    public static EmbeddedStorageFoundation<?> buildAzureFoundation(EclipseStoreStorageSettings settings, ADirectory directory) {
        BlobStoreFileSystem fileSystem = (BlobStoreFileSystem) directory.fileSystem();
        StorageConfiguration.Builder<?> builder = Storage.ConfigurationBuilder()
                .setStorageFileProvider(Storage.FileProviderBuilder(fileSystem)
                        .setDirectory(directory)
                        .setFileNameProvider(buildFileNameProvider(settings))
                        .createFileProvider())
                .setChannelCountProvider(StorageChannelCountProvider.New(
                        settings.getChannelCount() != null ? settings.getChannelCount() : StorageChannelCountProvider.Defaults.defaultChannelCount()))
                .setHousekeepingController(buildHousekeepingController(settings))
                .setEntityCacheEvaluator(buildEntityCacheEvaluator(settings))
                .setDataFileEvaluator(buildDataFileEvaluator(settings));

        return org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation.New()
                .setConfiguration(builder.createConfiguration());
    }

    public static EmbeddedStorageManager createAndStartStorageManager(EmbeddedStorageFoundation<?> foundation, Object initialRoot) {
        EmbeddedStorageManager storageManager = foundation.createEmbeddedStorageManager(initialRoot);
        storageManager.start();
        return storageManager;
    }

    public static String suffixedContainer(String baseName, String suffix) {
        return fallback(baseName, DEFAULT_AZURE_CONTAINER) + suffix;
    }

    public static String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static EclipseStoreStoreConfigPaths storePaths(EclipseStoreConfig config, String storeName) {
        if (config == null) {
            return null;
        }
        return switch (storeName) {
            case "routes" -> config.getRoutes();
            case "audit" -> config.getAudit();
            case "changelog" -> config.getChangelog();
            case "validationrule" -> config.getValidationrule();
            default -> null;
        };
    }

    private static String defaultLocalConfig(String storeName) {
        return switch (storeName) {
            case "routes" -> DEFAULT_ROUTE_LOCAL_CONFIG;
            case "audit" -> DEFAULT_AUDIT_LOCAL_CONFIG;
            case "changelog" -> DEFAULT_CHANGELOG_LOCAL_CONFIG;
            case "validationrule" -> DEFAULT_VALIDATIONRULE_LOCAL_CONFIG;
            default -> throw new IllegalArgumentException("Unknown EclipseStore config store '" + storeName + "'");
        };
    }

    private static String defaultAzureConfig(String storeName) {
        return switch (storeName) {
            case "routes" -> DEFAULT_ROUTE_AZURE_CONFIG;
            case "audit" -> DEFAULT_AUDIT_AZURE_CONFIG;
            case "changelog" -> DEFAULT_CHANGELOG_AZURE_CONFIG;
            case "validationrule" -> DEFAULT_VALIDATIONRULE_AZURE_CONFIG;
            default -> throw new IllegalArgumentException("Unknown EclipseStore config store '" + storeName + "'");
        };
    }

    private static ADirectory optionalDirectory(NioFileSystem fileSystem, String directory) {
        return directory == null || directory.isBlank() ? null : fileSystem.ensureDirectoryPath(directory);
    }

    private static StorageBackupSetup optionalBackupSetup(NioFileSystem fileSystem, String backupDirectory) {
        if (backupDirectory == null || backupDirectory.isBlank()) {
            return null;
        }
        return StorageBackupSetup.New(fileSystem.ensureDirectoryPath(backupDirectory));
    }

    private static StorageFileNameProvider buildFileNameProvider(EclipseStoreStorageSettings settings) {
        StorageFileNameProvider.Builder<?> builder = StorageFileNameProvider.Builder();
        if (settings.getChannelDirectoryPrefix() != null) {
            builder.setChannelDirectoryPrefix(settings.getChannelDirectoryPrefix());
        }
        if (settings.getDataFilePrefix() != null) {
            builder.setDataFilePrefix(settings.getDataFilePrefix());
        }
        if (settings.getDataFileSuffix() != null) {
            builder.setDataFileSuffix(settings.getDataFileSuffix());
        }
        if (settings.getTransactionFilePrefix() != null) {
            builder.setTransactionsFilePrefix(settings.getTransactionFilePrefix());
        }
        if (settings.getTransactionFileSuffix() != null) {
            builder.setTransactionsFileSuffix(settings.getTransactionFileSuffix());
        }
        if (settings.getRescuedFileSuffix() != null) {
            builder.setRescuedFileSuffix(settings.getRescuedFileSuffix());
        }
        if (settings.getTypeDictionaryFileName() != null) {
            builder.setTypeDictionaryFileName(settings.getTypeDictionaryFileName());
        }
        if (settings.getLockFileName() != null) {
            builder.setLockFileName(settings.getLockFileName());
        }
        return builder.createFileNameProvider();
    }

    private static void normalizeConfiguredSuffixes(EclipseStoreStorageSettings settings) {
        settings.setDataFileSuffix(stripLeadingDots(settings.getDataFileSuffix()));
        settings.setTransactionFileSuffix(stripLeadingDots(settings.getTransactionFileSuffix()));
        settings.setRescuedFileSuffix(stripLeadingDots(settings.getRescuedFileSuffix()));
    }

    private static String stripLeadingDots(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int index = 0;
        while (index < value.length() && value.charAt(index) == '.') {
            index++;
        }
        return index == value.length() ? value : value.substring(index);
    }

    private static void repairDoubleDotSuffixFiles(Path storageDirectory, EclipseStoreStorageSettings settings) {
        if (!Files.isDirectory(storageDirectory)) {
            return;
        }
        renameDoubleDotSuffixFiles(storageDirectory, settings.getDataFileSuffix());
        renameDoubleDotSuffixFiles(storageDirectory, settings.getTransactionFileSuffix());
        renameDoubleDotSuffixFiles(storageDirectory, settings.getRescuedFileSuffix());
    }

    private static void renameDoubleDotSuffixFiles(Path storageDirectory, String normalizedSuffix) {
        if (normalizedSuffix == null || normalizedSuffix.isBlank()) {
            return;
        }
        String doubleDotSuffix = ".." + normalizedSuffix;
        String singleDotSuffix = "." + normalizedSuffix;

        try (Stream<Path> paths = Files.walk(storageDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(doubleDotSuffix))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String normalizedName = fileName.substring(0, fileName.length() - doubleDotSuffix.length())
                                + singleDotSuffix;
                        Path target = path.resolveSibling(normalizedName);
                        if (Files.notExists(target)) {
                            try {
                                Files.move(path, target);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to normalize EclipseStore local file suffixes in " + storageDirectory, e);
        }
    }

    private static StorageHousekeepingController buildHousekeepingController(EclipseStoreStorageSettings settings) {
        long interval = settings.getHousekeepingInterval() != null
                ? settings.getHousekeepingInterval()
                : StorageHousekeepingController.Defaults.defaultHousekeepingIntervalMs();
        long budget = settings.getHousekeepingTimeBudget() != null
                ? settings.getHousekeepingTimeBudget()
                : StorageHousekeepingController.Defaults.defaultHousekeepingTimeBudgetNs();
        return Storage.HousekeepingController(interval, budget);
    }

    private static StorageEntityCacheEvaluator buildEntityCacheEvaluator(EclipseStoreStorageSettings settings) {
        if (settings.getEntityCacheTimeout() != null && settings.getEntityCacheThreshold() != null) {
            return Storage.EntityCacheEvaluator(settings.getEntityCacheTimeout(), settings.getEntityCacheThreshold());
        }
        if (settings.getEntityCacheTimeout() != null) {
            return Storage.EntityCacheEvaluator(settings.getEntityCacheTimeout());
        }
        return Storage.EntityCacheEvaluator();
    }

    private static StorageDataFileEvaluator buildDataFileEvaluator(EclipseStoreStorageSettings settings) {
        int minSize = settings.getDataFileMinimumSize() != null
                ? settings.getDataFileMinimumSize()
                : StorageDataFileEvaluator.Defaults.defaultFileMinimumSize();
        int maxSize = settings.getDataFileMaximumSize() != null
                ? settings.getDataFileMaximumSize()
                : StorageDataFileEvaluator.Defaults.defaultFileMaximumSize();
        double minUseRatio = settings.getDataFileMinimumUseRatio() != null
                ? settings.getDataFileMinimumUseRatio()
                : StorageDataFileEvaluator.Defaults.defaultMinimumUseRatio();
        boolean cleanupHeadFile = settings.getDataFileCleanupHeadFile() != null
                ? settings.getDataFileCleanupHeadFile()
                : StorageDataFileEvaluator.Defaults.defaultResolveHeadfile();
        int txMaxSize = settings.getTransactionFileMaximumSize() != null
                ? settings.getTransactionFileMaximumSize()
                : StorageDataFileEvaluator.Defaults.defaultTransactionFileMaximumSize();
        return Storage.DataFileEvaluator(minSize, maxSize, minUseRatio, cleanupHeadFile, txMaxSize);
    }
}

