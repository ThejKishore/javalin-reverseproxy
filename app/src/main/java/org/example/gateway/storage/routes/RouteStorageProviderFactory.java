/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.GatewayConfig;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the appropriate {@link RouteStorageProvider} based on
 * the {@code gateway.config-source} setting in {@code application.yml}.
 *
 * <table border="1">
 *   <tr><th>config-source</th><th>Provider</th></tr>
 *   <tr><td>database</td><td>{@link DatabaseRouteStorageProvider} (JDBI)</td></tr>
 *   <tr><td>eclipse-store-lcl</td><td>{@link EclipseStoreLocalRouteStorageProvider}</td></tr>
 *   <tr><td>eclipse-store-azure</td><td>{@link EclipseStoreAzureRouteStorageProvider}</td></tr>
 * </table>
 */
public class RouteStorageProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(RouteStorageProviderFactory.class);

    private RouteStorageProviderFactory() {}

    /**
     * Creates a {@link RouteStorageProvider} from the supplied gateway configuration.
     *
     * @param config full gateway configuration
     * @param jdbi   JDBI instance (used when config-source = database); may be null otherwise
     * @return the configured provider
     * @throws IllegalArgumentException if config-source is unknown
     * @throws IllegalStateException    if required EclipseStore config is missing
     */
    public static RouteStorageProvider create(GatewayConfig config, Jdbi jdbi) {
        String source = config.getConfigSource();
        log.info("Creating RouteStorageProvider for config-source='{}'", source);

        return switch (source.toLowerCase()) {
            case "database" -> {
                if (jdbi == null) throw new IllegalStateException("JDBI required for database storage");
                yield new DatabaseRouteStorageProvider(jdbi);
            }
            case "eclipse-store-lcl" -> {
                EclipseStoreConfig esConfig = config.getEclipseStore();
                String path = esConfig != null ? esConfig.getStoragePath() : "./eclipse-store-data";
                yield new EclipseStoreLocalRouteStorageProvider(path);
            }
            case "eclipse-store-azure" -> {
                EclipseStoreConfig esConfig = config.getEclipseStore();
                if (esConfig == null || esConfig.getAzureConnectionString() == null) {
                    throw new IllegalStateException(
                            "eclipse-store.azure-connection-string is required for eclipse-store-azure");
                }
                yield new EclipseStoreAzureRouteStorageProvider(
                        esConfig.getAzureConnectionString(),
                        esConfig.getAzureContainer());
            }
            default -> throw new IllegalArgumentException(
                    "Unknown config-source '" + source + "'. Valid values: database, eclipse-store-lcl, eclipse-store-azure");
        };
    }
}
