/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.EclipseStoreStoreConfigPaths;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.dao.TargetsItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("EclipseStore local route storage provider")
class EclipseStoreLocalRouteStorageProviderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("insert should persist and reload routes when the storage manager is started from a foundation")
    void insert_shouldPersistAndReloadRoutes_when_storageManagerCreatedFromFoundation() throws IOException {
        Path configFile = tempDir.resolve("routes-storage-local.yaml");
        Files.writeString(configFile, """
                storage-directory: "%s"
                channel-count: 1
                """.formatted(tempDir.toString()));

        EclipseStoreConfig config = new EclipseStoreConfig();
        config.setRoutes(new EclipseStoreStoreConfigPaths(configFile.toString(), "unused"));

        RouteDao route = new RouteDao(
                "/gateway",
                "/orders/*",
                1_500,
                List.of(new TargetsItem(null, 1, null, "http://localhost:9000")),
                true,
                null,
                null,
                "ROUND_ROBIN",
                List.of(),
                null,
                "PATH",
                null,
                "orders-route",
                "audit",
                "route-1",
                true,
                Map.of("owner", "test")
        );

        try (var provider = new EclipseStoreLocalRouteStorageProvider(config)) {
            assertAll(
                    () -> assertDoesNotThrow(() -> provider.insert(route)),
                    () -> assertEquals(List.of(route), provider.findAll()),
                    () -> assertEquals(List.of(route), provider.findAllEnabled())
            );
        }

        try (var reloadedProvider = new EclipseStoreLocalRouteStorageProvider(config)) {
            assertEquals(List.of(route), reloadedProvider.findAll());
        }
    }
}

