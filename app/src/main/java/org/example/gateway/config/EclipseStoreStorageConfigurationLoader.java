/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads per-store EclipseStore YAML configuration files.
 */
public final class EclipseStoreStorageConfigurationLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private EclipseStoreStorageConfigurationLoader() {
    }

    public static EclipseStoreStorageSettings load(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("EclipseStore config location must be configured");
        }

        try (InputStream stream = open(location)) {
            return YAML_MAPPER.readValue(stream, EclipseStoreStorageSettings.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load EclipseStore config from '" + location + "'", e);
        }
    }

    private static InputStream open(String location) throws IOException {
        Path path = Path.of(location);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }

        String classpathLocation = normalizeClasspathLocation(location);
        InputStream classpathStream = EclipseStoreStorageConfigurationLoader.class
                .getClassLoader()
                .getResourceAsStream(classpathLocation);
        if (classpathStream != null) {
            return classpathStream;
        }

        throw new IllegalStateException("EclipseStore config not found at '" + location + "'");
    }

    private static String normalizeClasspathLocation(String location) {
        if (location.startsWith("classpath:")) {
            return location.substring("classpath:".length()).replaceFirst("^/", "");
        }
        return location.replaceFirst("^/", "");
    }
}

