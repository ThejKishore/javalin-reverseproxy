/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import org.example.gateway.routes.dao.ValidationRuleEntry;

import java.util.List;

/**
 * Persistence contract for gateway validation-rule storage.
 *
 * <p>Implementations are selected at startup based on {@code config-source}:
 * <ul>
 *   <li>{@code database}            → {@link DatabaseValidationRuleStorageProvider} (JDBI)</li>
 *   <li>{@code eclipse-store-lcl}   → {@link EclipseStoreLocalValidationRuleStorageProvider}</li>
 *   <li>{@code eclipse-store-azure} → {@link EclipseStoreAzureValidationRuleStorageProvider}</li>
 * </ul>
 */
public interface ValidationRuleStorageProvider extends AutoCloseable {

    /** Returns all validation rules regardless of enabled state. */
    List<ValidationRuleEntry> findAll();

    /** Returns only rules with {@code enabled = true}. */
    List<ValidationRuleEntry> findAllEnabled();

    /**
     * Inserts a new validation rule.
     *
     * @param id      unique rule identifier
     * @param name    human-readable name
     * @param pattern regular expression pattern
     * @param target  one of {@code QUERY_PARAM}, {@code HEADER}, {@code COOKIE}, {@code BODY}, {@code ALL}
     * @param enabled whether the rule is active
     */
    void insert(String id, String name, String pattern, String target, boolean enabled);

    /**
     * Toggles the enabled flag for a rule.
     *
     * @return number of affected records (1 on success, 0 if not found)
     */
    int setEnabled(String id, boolean enabled);

    /**
     * Removes a rule permanently.
     *
     * @return number of affected records (1 on success, 0 if not found)
     */
    int deleteById(String id);

    /** Releases any resources held by this provider. */
    @Override
    default void close() {}
}
