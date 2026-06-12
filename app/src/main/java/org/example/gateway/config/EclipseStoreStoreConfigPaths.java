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
 * Points a logical store (routes, audit, changelog, validationrule) at its
 * local and Azure YAML configuration resources.
 */
public class EclipseStoreStoreConfigPaths {

    @JsonProperty("local-config")
    private String localConfig;

    @JsonProperty("azure-config")
    private String azureConfig;

    public EclipseStoreStoreConfigPaths() {
    }

    public EclipseStoreStoreConfigPaths(String localConfig, String azureConfig) {
        this.localConfig = localConfig;
        this.azureConfig = azureConfig;
    }

    public String getLocalConfig() { return localConfig; }
    public void setLocalConfig(String localConfig) { this.localConfig = localConfig; }

    public String getAzureConfig() { return azureConfig; }
    public void setAzureConfig(String azureConfig) { this.azureConfig = azureConfig; }
}

