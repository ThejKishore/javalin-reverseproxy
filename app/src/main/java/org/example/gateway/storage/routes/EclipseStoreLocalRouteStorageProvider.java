/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.dao.RoutesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * {@link RouteStorageProvider} backed by EclipseStore on the local filesystem.
 *
 * <p>The entire route list is stored as a binary object graph under
 * {@code storagePath} (default: {@code ./eclipse-store-data}).
 * EclipseStore serialises / deserialises Java objects natively — no JSON
 * is involved — making this option ideal for single-node deployments.
 *
 * <p>Thread safety is enforced via {@code synchronized} on the shared
 * {@link RoutesDao} list.
 */
public class EclipseStoreLocalRouteStorageProvider implements RouteStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreLocalRouteStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final RoutesDao root;

    public EclipseStoreLocalRouteStorageProvider(String storagePath) {
        Path path = Paths.get(storagePath);
        log.info("Starting EclipseStore local storage at '{}'", path.toAbsolutePath());

        // If this path already has persisted data, EmbeddedStorage restores the graph;
        // otherwise, it starts fresh with the provided root object.
        RoutesDao initialRoot = new RoutesDao();
        this.storageManager = EmbeddedStorage.start(initialRoot, path);

        // After start(), the manager's root() is the *restored* graph (or initialRoot if new).
        RoutesDao storedRoot = storageManager.root();
        this.root = storedRoot instanceof RoutesDao ecRoot ? ecRoot : initialRoot;

        log.info("EclipseStore local: loaded {} route(s)", root.getRoutes().size());
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
                            existing.auditStore(), existing.id(), existing.auditEnabled());
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
