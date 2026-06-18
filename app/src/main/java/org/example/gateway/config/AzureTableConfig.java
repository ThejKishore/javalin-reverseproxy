/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for Azure Table Storage backends.
 *
 * <p>Used when {@code config-source} is {@code azure-table}.
 *
 * <p>Authentication order:
 * <ol>
 *   <li>If {@code connection-string} is set, it is used directly (dev/test).</li>
 *   <li>Otherwise, {@code endpoint} is used with {@code DefaultAzureCredential}
 *       (recommended for production workloads running in Azure).</li>
 * </ol>
 *
 * <p>Table names follow the requirement:
 * <ul>
 *   <li>{@code routes}     → gateway route definitions</li>
 *   <li>{@code audit}      → request audit log</li>
 *   <li>{@code changelog}  → admin change log</li>
 *   <li>{@code validation} → HTTP validation rules</li>
 * </ul>
 */
public class AzureTableConfig {

    /**
     * Full Azure Storage connection string.
     * When set, this takes precedence over {@code endpoint} + DefaultAzureCredential.
     * Keep this value in an environment variable or secrets manager — never in source control.
     * Example: {@code DefaultEndpointsProtocol=https;AccountName=thejdatalake;AccountKey=...;EndpointSuffix=core.windows.net}
     */
    @JsonProperty("connection-string")
    private String connectionString;

    /**
     * Azure Table Storage endpoint URL.
     * Used when {@code connection-string} is absent (DefaultAzureCredential auth).
     */
    @JsonProperty("endpoint")
    private String endpoint = "https://thejdatalake.table.core.windows.net";

    /** Table name for route definitions. */
    @JsonProperty("routes-table")
    private String routesTable = "routes";

    /** Table name for audit log entries. */
    @JsonProperty("audit-table")
    private String auditTable = "audit";

    /** Table name for change log entries. */
    @JsonProperty("changelog-table")
    private String changelogTable = "changelog";

    /** Table name for HTTP validation rules. */
    @JsonProperty("validation-table")
    private String validationTable = "validation";

    // ── Getters & setters ─────────────────────────────────────────────────────

    public String getConnectionString() { return connectionString; }
    public void setConnectionString(String connectionString) { this.connectionString = connectionString; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getRoutesTable() { return routesTable; }
    public void setRoutesTable(String routesTable) { this.routesTable = routesTable; }

    public String getAuditTable() { return auditTable; }
    public void setAuditTable(String auditTable) { this.auditTable = auditTable; }

    public String getChangelogTable() { return changelogTable; }
    public void setChangelogTable(String changelogTable) { this.changelogTable = changelogTable; }

    public String getValidationTable() { return validationTable; }
    public void setValidationTable(String validationTable) { this.validationTable = validationTable; }
}
