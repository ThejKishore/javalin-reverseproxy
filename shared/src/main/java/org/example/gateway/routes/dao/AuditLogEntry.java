/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.routes.dao;

import java.time.Instant;

/**
 * Persistence POJO for an audit-log entry.
 *
 * <p>Used by all {@code AuditStorageProvider} implementations (JDBI, EclipseStore local,
 * EclipseStore Azure).  Mirrors {@code AuditLogRow} but as an immutable record so that
 * EclipseStore can serialise / deserialise it natively without JDBI annotations.
 */
public record AuditLogEntry(
        String id,
        String routeId,
        String routeName,
        String requestId,
        String traceId,
        String httpMethod,
        String requestPath,
        String upstreamUrl,
        Integer statusCode,
        Long durationMs,
        String clientIp,
        Instant createdAt
) {}
