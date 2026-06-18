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
 * Persistence POJO for a validation rule.
 *
 * <p>Used by all {@code ValidationRuleStorageProvider} implementations (JDBI and Azure Table Storage).
 * Mirrors {@code ValidationRuleRow} as an immutable record.
 *
 * <p>The {@code target} field is one of: {@code QUERY_PARAM}, {@code HEADER},
 * {@code COOKIE}, {@code BODY}, {@code ALL}.
 */
public record ValidationRuleEntry(
        String id,
        String name,
        String pattern,
        String target,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
