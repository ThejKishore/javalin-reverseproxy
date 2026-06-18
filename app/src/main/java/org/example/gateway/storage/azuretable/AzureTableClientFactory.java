/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.azuretable;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.example.gateway.config.AzureTableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates Azure {@link TableClient} instances from {@link AzureTableConfig}.
 *
 * <p>Authentication strategy:
 * <ul>
 *   <li>If {@code connection-string} is configured → uses the connection string directly.</li>
 *   <li>Otherwise → uses {@code DefaultAzureCredential} (managed identity, environment
 *       variables, Azure CLI, etc.) with the configured {@code endpoint}.</li>
 * </ul>
 *
 * <p>Tables are created automatically on first use if they do not exist.
 */
public class AzureTableClientFactory {

    private static final Logger log = LoggerFactory.getLogger(AzureTableClientFactory.class);

    private final AzureTableConfig config;
    private final TableServiceClient serviceClient;

    public AzureTableClientFactory(AzureTableConfig config) {
        this.config = config;
        this.serviceClient = buildServiceClient(config);
    }

    /**
     * Returns a {@link TableClient} for the given table name.
     * Creates the table if it does not already exist.
     */
    public TableClient clientFor(String tableName) {
        serviceClient.createTableIfNotExists(tableName);
        TableClient client = serviceClient.getTableClient(tableName);
        log.debug("Azure Table client ready for table '{}'", tableName);
        return client;
    }

    /** Returns a pre-built client for the routes table. */
    public TableClient routesClient() {
        return clientFor(config.getRoutesTable());
    }

    /** Returns a pre-built client for the audit table. */
    public TableClient auditClient() {
        return clientFor(config.getAuditTable());
    }

    /** Returns a pre-built client for the changelog table. */
    public TableClient changelogClient() {
        return clientFor(config.getChangelogTable());
    }

    /** Returns a pre-built client for the validation rules table. */
    public TableClient validationClient() {
        return clientFor(config.getValidationTable());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static TableServiceClient buildServiceClient(AzureTableConfig config) {
        TableServiceClientBuilder builder = new TableServiceClientBuilder();
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            log.info("Azure Table Storage: using connection string authentication");
            builder.connectionString(config.getConnectionString());
        } else {
            log.info("Azure Table Storage: using DefaultAzureCredential (endpoint={})", config.getEndpoint());
            builder.endpoint(config.getEndpoint())
                   .credential(new DefaultAzureCredentialBuilder().build());
        }
        return builder.buildClient();
    }
}
