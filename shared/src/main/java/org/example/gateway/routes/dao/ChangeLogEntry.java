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
 * Persistence POJO for a change-log entry.
 *
 * <p>Used by all {@code ChangeLogStorageProvider} implementations (JDBI, EclipseStore local,
 * EclipseStore Azure).  Mirrors {@code ChangeLogRow} but as an immutable record so that
 * EclipseStore can serialise / deserialise it natively without JDBI annotations.
 */
public record ChangeLogEntry(
        String id,
        String action,
        String routeId,
        String routeName,
        String details,
        String performedBy,
        Instant createdAt
) {}
