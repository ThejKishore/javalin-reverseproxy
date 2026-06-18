/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.audit;

import org.example.gateway.config.AzureTableConfig;
import org.example.gateway.config.GatewayConfig;
import org.example.gateway.storage.azuretable.AzureTableClientFactory;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the appropriate {@link AuditStorageProvider} based on the
 * {@code gateway.config-source} setting in {@code application.yml}.
 *
 * <table border="1">
 *   <tr><th>config-source</th><th>Provider</th></tr>
 *   <tr><td>database</td><td>{@link DatabaseAuditStorageProvider}</td></tr>
 *   <tr><td>azure-table</td><td>{@link AzureTableAuditStorageProvider}</td></tr>
 * </table>
 */
public class AuditStorageProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AuditStorageProviderFactory.class);

    private AuditStorageProviderFactory() {}

    /**
     * Creates an {@link AuditStorageProvider} from the supplied gateway configuration.
     *
     * @param config full gateway configuration
     * @param jdbi   JDBI instance (used when config-source = database); may be null otherwise
     * @return the configured provider
     */
    public static AuditStorageProvider create(GatewayConfig config, Jdbi jdbi) {
        String source = config.getConfigSource();
        log.info("Creating AuditStorageProvider for config-source='{}'", source);

        return switch (source.toLowerCase()) {
            case "database" -> {
                if (jdbi == null) throw new IllegalStateException("JDBI required for database storage");
                yield new DatabaseAuditStorageProvider(jdbi);
            }
            case "azure-table" -> {
                AzureTableConfig atConfig = config.getAzureTable();
                if (atConfig == null) {
                    throw new IllegalStateException("azure-table config block is required for azure-table storage");
                }
                yield new AzureTableAuditStorageProvider(new AzureTableClientFactory(atConfig));
            }
            default -> throw new IllegalArgumentException(
                    "Unknown config-source '" + source + "'");
        };
    }
}
