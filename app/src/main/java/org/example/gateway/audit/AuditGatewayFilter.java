/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.audit;

import org.example.gateway.db.AuditDao;
import org.example.gateway.db.AuditLogRow;
import org.example.utilities.gateway.filter.FilterChain;
import org.example.utilities.gateway.filter.FilterContext;
import org.example.utilities.gateway.filter.GatewayFilter;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Wraps the entire filter chain to record a structured audit log entry.
 *
 * <p>When {@code auditStore=database} the record is persisted via JDBI.
 * In all cases a SLF4J info line is emitted for log aggregation tools.
 */
public class AuditGatewayFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditGatewayFilter.class);

    private final Jdbi jdbi;
    private final String auditStore; // "database" or "file"

    public AuditGatewayFilter(Jdbi jdbi, String auditStore) {
        this.jdbi = jdbi;
        this.auditStore = auditStore;
    }

    @Override
    public void filter(FilterContext ctx, FilterChain chain) throws Exception {
        Exception thrown = null;
        try {
            chain.proceed(ctx);
        } catch (Exception ex) {
            thrown = ex;
            throw ex;
        } finally {
            persist(ctx);
        }
    }

    private void persist(FilterContext ctx) {
        try {
            AuditLogRow row = new AuditLogRow();
            row.setId(UUID.randomUUID().toString());
            row.setRouteId(ctx.getRouteDefinition().getId());
            row.setRouteName(ctx.getRouteDefinition().getName());
            row.setRequestId(ctx.getRequestId());
            row.setTraceId(ctx.getTraceId());
            row.setHttpMethod(ctx.getJavalinCtx().method().name());
            row.setRequestPath(ctx.getJavalinCtx().path());
            row.setUpstreamUrl(ctx.getResolvedUpstreamUrl());
            row.setStatusCode(ctx.getUpstreamStatusCode());
            row.setDurationMs(ctx.elapsedMs());
            row.setClientIp(ctx.getJavalinCtx().ip());

            log.info("AUDIT method={} path={} route={} upstream={} status={} durationMs={} requestId={}",
                    row.getHttpMethod(), row.getRequestPath(), row.getRouteName(),
                    row.getUpstreamUrl(), row.getStatusCode(), row.getDurationMs(), row.getRequestId());

            if ("database".equalsIgnoreCase(auditStore) && jdbi != null) {
                jdbi.useExtension(AuditDao.class, dao -> dao.insert(row));
            }
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}

