/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.changelog;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/** JDBI SqlObject DAO for the {@code change_logs} table. */
@RegisterBeanMapper(ChangeLogRow.class)
public interface ChangeLogDao {

    @SqlUpdate("INSERT INTO change_logs (id, action, route_id, route_name, details, performed_by) " +
               "VALUES (:id, :action, :routeId, :routeName, :details, :performedBy)")
    void insert(@BindBean ChangeLogRow row);

    @SqlQuery("SELECT * FROM change_logs ORDER BY created_at DESC LIMIT :limit")
    List<ChangeLogRow> findRecent(@Bind("limit") int limit);

    @SqlQuery("SELECT * FROM change_logs WHERE route_id = :routeId ORDER BY created_at DESC LIMIT :limit")
    List<ChangeLogRow> findByRouteId(@Bind("routeId") String routeId, @Bind("limit") int limit);
}

