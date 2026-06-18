/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.example.gateway.routes.dao.RouteDao;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for gateway route storage.
 *
 * <p>Implementations are selected at startup based on {@code config-source}:
 * <ul>
 *   <li>{@code database}    → {@link DatabaseRouteStorageProvider} (JDBI)</li>
 *   <li>{@code azure-table} → Azure Table Storage implementation</li>
 * </ul>
 *
 * <p>All methods operate on {@link RouteDao} POJOs (shared module), keeping
 * the persistence layer independent from both the API layer ({@code RouteDto})
 * and the runtime model ({@code RouteDefinition}).
 */
public interface RouteStorageProvider extends AutoCloseable {

    /** Returns all routes regardless of enabled state. */
    List<RouteDao> findAll();

    /** Returns only routes with {@code enabled = true}. */
    List<RouteDao> findAllEnabled();

    /** Finds a single route by its unique path-pattern key. */
    Optional<RouteDao> findById(String id);

    /** Inserts a new route. */
    void insert(RouteDao route);

    /**
     * Replaces an existing route's configuration.
     *
     * @return number of affected records (1 on success, 0 if not found)
     */
    int update(RouteDao route);

    /**
     * Toggles the enabled flag for a route.
     *
     * @return number of affected records (1 on success, 0 if not found)
     */
    int setEnabled(String id, long version, boolean enabled);

    /**
     * Removes a route permanently.
     *
     * @return number of affected records (1 on success, 0 if not found)
     */
    int deleteById(String id);

    /** Releases any resources held by this provider. */
    @Override
    default void close() {}
}
