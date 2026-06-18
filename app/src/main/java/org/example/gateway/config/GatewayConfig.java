/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.utilities.gateway.model.CspPolicy;
import org.example.utilities.gateway.model.CsrfPolicy;
import org.example.utilities.gateway.model.JwtPolicy;
import org.example.utilities.gateway.model.RouteDefinition;

import java.util.ArrayList;
import java.util.List;


/**
 * Top-level gateway configuration, populated from the {@code gateway:} block
 * in {@code application.yml}.
 */
public class GatewayConfig {

    @JsonProperty("port")
    private int port = 8080;

    /**
     * Where routes are loaded from: {@code yaml} (application.yml) or
     * {@code database} (JDBI / H2 / Postgres).
     */
    @JsonProperty("config-source")
    private String configSource = "yaml";

    /** How often (seconds) the gateway polls for route changes. */
    @JsonProperty("refresh-interval-seconds")
    private int refreshIntervalSeconds = 60;

    @JsonProperty("datasource")
    private DataSourceConfig datasource = new DataSourceConfig();

    /** Hazelcast cluster configuration for distributed CSRF token storage. */
    @JsonProperty("hazelcast")
    private HazelcastConfig hazelcast = new HazelcastConfig();

    /** Azure Table Storage configuration (used when config-source = azure-table). */
    @JsonProperty("azure-table")
    private AzureTableConfig azureTable = new AzureTableConfig();

    /**
     * Global JWT policy applied to every route by default.
     * A per-route {@code jwt-policy} overrides this (or disables it when
     * {@code enabled: false}).  Null means no global enforcement.
     */
    @JsonProperty("jwt-policy")
    private JwtPolicy jwtPolicy;

    /**
     * Global CSRF policy applied to every route by default.
     * Per-route {@code csrf-policy} overrides this.  Null means no global enforcement.
     */
    @JsonProperty("csrf-policy")
    private CsrfPolicy csrfPolicy;

    /**
     * Global CSP policy applied to every route by default.
     * Per-route {@code csp-policy} overrides this.  Null means no global enforcement.
     */
    @JsonProperty("csp-policy")
    private CspPolicy cspPolicy;

    /** Consolidated OpenAPI endpoint settings (path + access token auth). */
    @JsonProperty("openapi-export")
    private OpenApiExportConfig openApiExport = new OpenApiExportConfig();

    /** Inline route definitions — only used when config-source=yaml. */
    @JsonProperty("routes")
    private List<RouteDefinition> routes = new ArrayList<>();

    // --- Getters & setters ---

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getConfigSource() { return configSource; }
    public void setConfigSource(String configSource) { this.configSource = configSource; }

    public int getRefreshIntervalSeconds() { return refreshIntervalSeconds; }
    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) { this.refreshIntervalSeconds = refreshIntervalSeconds; }

    public DataSourceConfig getDatasource() { return datasource; }
    public void setDatasource(DataSourceConfig datasource) { this.datasource = datasource; }

    public HazelcastConfig getHazelcast() { return hazelcast; }
    public void setHazelcast(HazelcastConfig hazelcast) { this.hazelcast = hazelcast; }

    public AzureTableConfig getAzureTable() { return azureTable; }
    public void setAzureTable(AzureTableConfig azureTable) { this.azureTable = azureTable; }

    public JwtPolicy getJwtPolicy() { return jwtPolicy; }
    public void setJwtPolicy(JwtPolicy jwtPolicy) { this.jwtPolicy = jwtPolicy; }

    public CsrfPolicy getCsrfPolicy() { return csrfPolicy; }
    public void setCsrfPolicy(CsrfPolicy csrfPolicy) { this.csrfPolicy = csrfPolicy; }

    public CspPolicy getCspPolicy() { return cspPolicy; }
    public void setCspPolicy(CspPolicy cspPolicy) { this.cspPolicy = cspPolicy; }

    public OpenApiExportConfig getOpenApiExport() { return openApiExport; }
    public void setOpenApiExport(OpenApiExportConfig openApiExport) { this.openApiExport = openApiExport; }

    public List<RouteDefinition> getRoutes() { return routes; }
    public void setRoutes(List<RouteDefinition> routes) { this.routes = routes; }
}
