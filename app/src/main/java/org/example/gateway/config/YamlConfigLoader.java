/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * Loads {@link GatewayConfig} from {@code application.yml} on the classpath or
 * from a file on the local filesystem. Re-loadable at runtime to support
 * hot-reload of route definitions.
 */
public class YamlConfigLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Path externalConfigPath;

    /** Loads from the classpath only. */
    public YamlConfigLoader() {
        this.externalConfigPath = null;
    }

    /** Loads from an external file path, falling back to the classpath. */
    public YamlConfigLoader(Path externalConfigPath) {
        this.externalConfigPath = externalConfigPath;
    }

    /**
     * Reads and returns the gateway configuration.
     * The YAML file must have a top-level {@code gateway:} key.
     */
    public GatewayConfig load() throws IOException {
        try (InputStream is = openStream()) {
            JsonNode root = YAML_MAPPER.readTree(is);
            JsonNode gatewayNode = root.get("gateway");
            if (gatewayNode == null) {
                throw new IllegalStateException(
                        "application.yml must contain a top-level 'gateway:' key");
            }
            return YAML_MAPPER.treeToValue(gatewayNode, GatewayConfig.class);
        }
    }

    private InputStream openStream() throws IOException {
        if (externalConfigPath != null && Files.exists(externalConfigPath)) {
            return Files.newInputStream(externalConfigPath);
        }
        InputStream cp = getClass().getClassLoader().getResourceAsStream("application.yml");
        if (cp == null) {
            throw new IllegalStateException(
                    "application.yml not found on classpath and no external path configured");
        }
        return cp;
    }

    /** Shared mapper — useful for serialising {@code RouteDefinition} to/from JSON for DB storage. */
    public static ObjectMapper jsonMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}

