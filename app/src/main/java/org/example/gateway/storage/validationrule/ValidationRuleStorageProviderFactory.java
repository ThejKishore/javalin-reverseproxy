/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.GatewayConfig;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the appropriate {@link ValidationRuleStorageProvider} based on the
 * {@code gateway.config-source} setting in {@code application.yml}.
 *
 * <table border="1">
 *   <tr><th>config-source</th><th>Provider</th></tr>
 *   <tr><td>database</td><td>{@link DatabaseValidationRuleStorageProvider}</td></tr>
 *   <tr><td>eclipse-store-lcl</td><td>{@link EclipseStoreLocalValidationRuleStorageProvider}</td></tr>
 *   <tr><td>eclipse-store-azure</td><td>{@link EclipseStoreAzureValidationRuleStorageProvider}</td></tr>
 * </table>
 */
public class ValidationRuleStorageProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(ValidationRuleStorageProviderFactory.class);

    private ValidationRuleStorageProviderFactory() {}

    /**
     * Creates a {@link ValidationRuleStorageProvider} from the supplied gateway configuration.
     *
     * @param config full gateway configuration
     * @param jdbi   JDBI instance (used when config-source = database); may be null otherwise
     * @return the configured provider
     */
    public static ValidationRuleStorageProvider create(GatewayConfig config, Jdbi jdbi) {
        String source = config.getConfigSource();
        log.info("Creating ValidationRuleStorageProvider for config-source='{}'", source);

        return switch (source.toLowerCase()) {
            case "database" -> {
                if (jdbi == null) throw new IllegalStateException("JDBI required for database storage");
                yield new DatabaseValidationRuleStorageProvider(jdbi);
            }
            case "eclipse-store-lcl" -> {
                EclipseStoreConfig esConfig = config.getEclipseStore();
                yield new EclipseStoreLocalValidationRuleStorageProvider(esConfig);
            }
            case "eclipse-store-azure" -> {
                EclipseStoreConfig esConfig = config.getEclipseStore();
                if (esConfig == null || esConfig.getAzureConnectionString() == null) {
                    throw new IllegalStateException(
                            "eclipse-store.azure-connection-string is required for eclipse-store-azure");
                }
                yield new EclipseStoreAzureValidationRuleStorageProvider(esConfig);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown config-source '" + source + "'");
        };
    }
}
