/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.db;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/** JDBI SqlObject DAO for the {@code audit_logs} table. */
@RegisterBeanMapper(AuditLogRow.class)
public interface AuditDao {

    @SqlUpdate("INSERT INTO audit_logs " +
               "(id, route_id, route_name, request_id, trace_id, http_method, request_path, " +
               " upstream_url, status_code, duration_ms, client_ip) " +
               "VALUES (:id, :routeId, :routeName, :requestId, :traceId, :httpMethod, " +
               "        :requestPath, :upstreamUrl, :statusCode, :durationMs, :clientIp)")
    void insert(@BindBean AuditLogRow row);

    @SqlQuery("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit")
    List<AuditLogRow> findRecent(@Bind("limit") int limit);

    @SqlQuery("SELECT * FROM audit_logs WHERE route_id = :routeId ORDER BY created_at DESC LIMIT :limit")
    List<AuditLogRow> findByRouteId(@Bind("routeId") String routeId, @Bind("limit") int limit);

    @SqlQuery("SELECT * FROM audit_logs WHERE request_id = :requestId")
    List<AuditLogRow> findByRequestId(@Bind("requestId") String requestId);
}

