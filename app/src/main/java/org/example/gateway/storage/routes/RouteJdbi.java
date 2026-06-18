/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.routes;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

/**
 * JDBI SqlObject DAO for the {@code routes} table.
 * Routes are stored with their full configuration serialised as JSON in
 * the {@code config_json} column and keyed by {@code path-pattern}.
 */
@RegisterBeanMapper(RouteRow.class)
public interface RouteJdbi {

    @SqlQuery("SELECT id, name, config_json, enabled, version, created_at, updated_at FROM routes WHERE enabled = TRUE ORDER BY created_at")
    List<RouteRow> findAllEnabled();

    @SqlQuery("SELECT id, name, config_json, enabled, version, created_at, updated_at FROM routes ORDER BY created_at")
    List<RouteRow> findAll();

    @SqlQuery("SELECT id, name, config_json, enabled, version, created_at, updated_at FROM routes WHERE id = :id")
    Optional<RouteRow> findById(@Bind("id") String id);

    @SqlUpdate("INSERT INTO routes (id, name, config_json, enabled, version, created_at, updated_at) " +
               "VALUES (:id, :name, :configJson, :enabled, :version, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    void insert(@Bind("id") String id,
                @Bind("name") String name,
                @Bind("configJson") String configJson,
                @Bind("enabled") boolean enabled,
                @Bind("version") long version);

    @SqlUpdate("UPDATE routes SET name = :name, config_json = :configJson, " +
               "enabled = :enabled, version = :version + 1, updated_at = CURRENT_TIMESTAMP " +
               "WHERE id = :id AND version = :version")
    int update(@Bind("id") String id,
               @Bind("name") String name,
               @Bind("configJson") String configJson,
               @Bind("enabled") boolean enabled,
               @Bind("version") long version);

    @SqlUpdate("UPDATE routes SET enabled = :enabled, version = :version + 1, updated_at = CURRENT_TIMESTAMP " +
               "WHERE id = :id AND version = :version")
    int setEnabled(@Bind("id") String id, @Bind("version") long version, @Bind("enabled") boolean enabled);

    @SqlUpdate("DELETE FROM routes WHERE id = :id")
    int deleteById(@Bind("id") String id);
}
