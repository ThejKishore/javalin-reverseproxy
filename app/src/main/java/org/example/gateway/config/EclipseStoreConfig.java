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
 * Configuration for EclipseStore persistence backends.
 * Used when {@code config-source} is {@code eclipse-store-lcl} or {@code eclipse-store-azure}.
 */
public class EclipseStoreConfig {

    /** Local filesystem storage path (used when config-source = eclipse-store-lcl). */
    @JsonProperty("storage-path")
    private String storagePath = "./eclipse-store-data";

    /** Azure Blob Storage connection string (used when config-source = eclipse-store-azure). */
    @JsonProperty("azure-connection-string")
    private String azureConnectionString;

    /** Azure Blob Storage container name (used when config-source = eclipse-store-azure). */
    @JsonProperty("azure-container")
    private String azureContainer = "gateway-routes";

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getAzureConnectionString() { return azureConnectionString; }
    public void setAzureConnectionString(String azureConnectionString) {
        this.azureConnectionString = azureConnectionString;
    }

    public String getAzureContainer() { return azureContainer; }
    public void setAzureContainer(String azureContainer) { this.azureContainer = azureContainer; }
}
