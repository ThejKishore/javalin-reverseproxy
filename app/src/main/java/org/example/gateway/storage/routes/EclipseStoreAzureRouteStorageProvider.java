/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.eclipse.serializer.afs.types.ADirectory;
import org.eclipse.store.afs.blobstore.types.BlobStoreFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.example.gateway.config.EclipseStoreConfig;
import org.example.gateway.config.EclipseStoreConfigSupport;
import org.example.gateway.config.EclipseStoreStorageSettings;
import org.example.gateway.routes.dao.RouteDao;
import org.example.gateway.routes.dao.RoutesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * {@link RouteStorageProvider} backed by EclipseStore on Azure Blob Storage.
 *
 * <p>Requires the {@code azure-connection-string} and {@code azure-container}
 * fields in the {@code eclipse-store} config block of {@code application.yml}.
 *
 * <p>The EclipseStore binary object graph is persisted as blobs in the
 * configured Azure container, enabling multi-node gateway clusters that share
 * a single route store.
 *
 * <p>Thread safety is enforced via {@code synchronized} on the shared
 * {@link RoutesDao} list.
 */
public class EclipseStoreAzureRouteStorageProvider implements RouteStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(EclipseStoreAzureRouteStorageProvider.class);

    private final EmbeddedStorageManager storageManager;
    private final RoutesDao root;

    public EclipseStoreAzureRouteStorageProvider(EclipseStoreConfig config) {
        EclipseStoreStorageSettings settings = EclipseStoreConfigSupport.loadAzureSettings(config, "routes");
        String resolvedContainer = EclipseStoreConfigSupport.fallback(
                settings.getAzureContainer(), EclipseStoreConfigSupport.DEFAULT_AZURE_CONTAINER);
        log.info("Starting EclipseStore Azure storage (container='{}')", resolvedContainer);

        BlobStoreFileSystem fileSystem = EclipseStoreConfigSupport.newAzureFileSystem(settings);
        ADirectory directory = fileSystem.ensureDirectoryPath(resolvedContainer);

        RoutesDao initialRoot = new RoutesDao();
        this.storageManager = EclipseStoreConfigSupport.createAndStartStorageManager(
                EclipseStoreConfigSupport.buildAzureFoundation(settings, directory),
                initialRoot);

        Object storedRoot = storageManager.root();
        this.root = storedRoot instanceof RoutesDao ecRoot ? ecRoot : initialRoot;

        log.info("EclipseStore Azure: loaded {} route(s)", root.getRoutes().size());
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
        log.debug("EclipseStore Azure: inserted route '{}'", route.id());
    }

    @Override
    public int update(RouteDao route) {
        synchronized (root) {
            List<RouteDao> routes = root.getRoutes();
            for (int i = 0; i < routes.size(); i++) {
                if (route.id().equals(routes.get(i).id())) {
                    routes.set(i, route);
                    storageManager.store(routes);
                    log.debug("EclipseStore Azure: updated route '{}'", route.id());
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
                    log.debug("EclipseStore Azure: set route '{}' enabled={}", id, enabled);
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
                log.debug("EclipseStore Azure: deleted route '{}'", id);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void close() {
        storageManager.shutdown();
        log.info("EclipseStore Azure storage shut down");
    }
}
