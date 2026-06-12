/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.EclipseStoreConfigSupport;
import org.example.gateway.config.EclipseStoreStorageSettings;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.dao.RoutesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * {@link RouteStorageProvider} backed by EclipseStore on the local filesystem.
 *
 * <p>Route data is stored under {@code storagePath/routes} (e.g.
 * {@code ./eclipse-store-data/routes}), keeping it isolated from the
 * audit, changelog, and validation-rule sub-stores that share the same
 * base directory.  EclipseStore does not allow two {@code EmbeddedStorageManager}
 * instances to share an overlapping directory hierarchy, so each concern
 * must live in its own leaf directory.
 *
 * <p>A one-time automatic migration moves any existing root-level EclipseStore
 * data into the {@code routes/} subdirectory on first startup.
 *
 * <p>Thread safety is enforced via {@code synchronized} on the shared
 * {@link RoutesDao} list.
 */
public class EclipseStoreLocalRouteStorageProvider implements RouteStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreLocalRouteStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final RoutesDao root;

    public EclipseStoreLocalRouteStorageProvider(EclipseStoreConfig config) {
        EclipseStoreStorageSettings settings = EclipseStoreConfigSupport.loadLocalSettings(config, "routes");
        String storagePath = EclipseStoreConfigSupport.fallback(
                settings.getStorageDirectory(), EclipseStoreConfigSupport.DEFAULT_STORAGE_DIRECTORY);
        Path configuredDir = Paths.get(storagePath);
        migrateRootDataIfPresent(configuredDir);
        Path effectiveStorageDir = resolveEffectiveRouteStorageDirectory(configuredDir);
        settings.setStorageDirectory(effectiveStorageDir.toString());
        log.info("Starting EclipseStore local storage using config '{}'",
                EclipseStoreConfigSupport.localConfigPath(config, "routes"));

        // If this path already has persisted data, EmbeddedStorage restores the graph;
        // otherwise, it starts fresh with the provided root object.
        RoutesDao initialRoot = new RoutesDao();
        this.storageManager = EclipseStoreConfigSupport.createAndStartStorageManager(
                org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation.New()
                        .setConfiguration(EclipseStoreConfigSupport.buildLocalStorageConfiguration(settings)),
                initialRoot);

        // After start(), the manager's root() is the *restored* graph (or initialRoot if new).
        RoutesDao storedRoot = storageManager.root();
        this.root = storedRoot instanceof RoutesDao ecRoot ? ecRoot : initialRoot;

        log.info("EclipseStore local: loaded {} route(s)", root.getRoutes().size());
    }

    /**
     * One-time migration: if old root-level EclipseStore route data exists
     * (identified by the presence of {@code PersistenceTypeDictionary.ptd} at the
     * base directory, which means routes were previously stored at the root), move
     * all EclipseStore files into the new {@code routes/} subdirectory.
     *
     * <p>Other known sub-stores ({@code audit/}, {@code changelog/},
     * {@code validationrule/}) are left untouched.
     */
    private static void migrateRootDataIfPresent(Path storageDir) {
        Path routesDir = storageDir.resolve("routes");
        Path typeDictionary = storageDir.resolve("PersistenceTypeDictionary.ptd");

        // Already migrated, or no old root-level data to migrate.
        if (!isSharedRootLayout(storageDir) || Files.exists(routesDir) || !Files.exists(typeDictionary)) {
            return;
        }

        log.info("Detected legacy EclipseStore route data at '{}' — migrating to '{}'",
                storageDir, routesDir);
        try {
            Files.createDirectories(routesDir);

            // Move the type dictionary file.
            Files.move(typeDictionary,
                    routesDir.resolve("PersistenceTypeDictionary.ptd"),
                    StandardCopyOption.REPLACE_EXISTING);

            // Move every directory that is NOT one of the other known sub-stores.
            try (var stream = Files.list(storageDir)) {
                stream.filter(p -> {
                            String name = p.getFileName().toString();
                            return Files.isDirectory(p)
                                    && !name.equals("routes")
                                    && !name.equals("audit")
                                    && !name.equals("changelog")
                                    && !name.equals("validationrule");
                        })
                        .forEach(dir -> {
                            try {
                                Files.move(dir,
                                        routesDir.resolve(dir.getFileName()),
                                        StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }

            // Move lock file if present.
            Path lockFile = storageDir.resolve("used.lock");
            if (Files.exists(lockFile)) {
                Files.move(lockFile, routesDir.resolve("used.lock"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Migration complete: route data is now at '{}'", routesDir);
        } catch (IOException e) {
            log.error("Migration failed — please manually move route data from '{}' to '{}'",
                    storageDir, routesDir);
            throw new UncheckedIOException("EclipseStore route data migration failed", e);
        }
    }

    private static Path resolveEffectiveRouteStorageDirectory(Path configuredDir) {
        Path nestedRoutesDir = configuredDir.resolve("routes");
        boolean configuredStoreExists = Files.exists(configuredDir.resolve("PersistenceTypeDictionary.ptd"));
        boolean nestedStoreExists = Files.exists(nestedRoutesDir.resolve("PersistenceTypeDictionary.ptd"));
        if (!configuredStoreExists && nestedStoreExists) {
            log.info("Using nested route store '{}' for backward compatibility", nestedRoutesDir);
            return nestedRoutesDir;
        }
        return configuredDir;
    }

    private static boolean isSharedRootLayout(Path storageDir) {
        return Files.isDirectory(storageDir.resolve("audit"))
                || Files.isDirectory(storageDir.resolve("changelog"))
                || Files.isDirectory(storageDir.resolve("validationrule"));
    }


    @Override
    public List<RouteDao> findAll() {
        synchronized (root) {
            return List.copyOf(root.getRoutes());
        }
    }

    @Override
    public List<RouteDao> findAllEnabled() {
        synchronized (root) {
            return root.getRoutes().stream()
                    .filter(RouteDao::enabled)
                    .toList();
        }
    }

    @Override
    public Optional<RouteDao> findById(String id) {
        synchronized (root) {
            return root.getRoutes().stream()
                    .filter(r -> id.equals(r.id()))
                    .findFirst();
        }
    }

    @Override
    public void insert(RouteDao route) {
        synchronized (root) {
            root.getRoutes().add(route);
            storageManager.store(root.getRoutes());
        }
        log.debug("EclipseStore local: inserted route '{}'", route.id());
    }

    @Override
    public int update(RouteDao route) {
        synchronized (root) {
            List<RouteDao> routes = root.getRoutes();
            for (int i = 0; i < routes.size(); i++) {
                if (route.id().equals(routes.get(i).id())) {
                    routes.set(i, route);
                    storageManager.store(routes);
                    log.debug("EclipseStore local: updated route '{}'", route.id());
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public int setEnabled(String id, boolean enabled) {
        synchronized (root) {
            List<RouteDao> routes = root.getRoutes();
            for (int i = 0; i < routes.size(); i++) {
                RouteDao existing = routes.get(i);
                if (id.equals(existing.id())) {
                    RouteDao updated = new RouteDao(
                            existing.stripPrefix(), existing.pathPattern(), existing.timeoutMs(),
                            existing.targets(), enabled, existing.headerRules(),
                            existing.circuitBreakerPolicy(), existing.loadBalancerType(),
                            existing.authForwardHeaders(), existing.rateLimitPolicy(),
                            existing.routingType(), existing.cachePolicy(), existing.name(),
                            existing.auditStore(), existing.id(), existing.auditEnabled(), existing.metaData());
                    routes.set(i, updated);
                    storageManager.store(routes);
                    log.debug("EclipseStore local: set route '{}' enabled={}", id, enabled);
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public int deleteById(String id) {
        synchronized (root) {
            boolean removed = root.getRoutes().removeIf(r -> id.equals(r.id()));
            if (removed) {
                storageManager.store(root.getRoutes());
                log.debug("EclipseStore local: deleted route '{}'", id);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void close() {
        storageManager.shutdown();
        log.info("EclipseStore local storage shut down");
    }
}
