/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import org.example.gateway.routes.dao.ValidationRuleEntry;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@link ValidationRuleStorageProvider} backed by JDBI (H2 / PostgreSQL).
 *
 * <p>Converts between the JDBI {@link ValidationRuleRow} bean and the shared
 * {@link ValidationRuleEntry} record on every read/write.
 */
public class DatabaseValidationRuleStorageProvider implements ValidationRuleStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(DatabaseValidationRuleStorageProvider.class);

    private final Jdbi jdbi;

    public DatabaseValidationRuleStorageProvider(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public List<ValidationRuleEntry> findAll() {
        return jdbi.withExtension(ValidationRuleDao.class, dao ->
                dao.findAll().stream().map(this::toEntry).toList());
    }

    @Override
    public List<ValidationRuleEntry> findAllEnabled() {
        return jdbi.withExtension(ValidationRuleDao.class, dao ->
                dao.findAllEnabled().stream().map(this::toEntry).toList());
    }

    @Override
    public void insert(String id, String name, String pattern, String target, boolean enabled) {
        jdbi.useExtension(ValidationRuleDao.class, dao ->
                dao.insert(id, name, pattern, target, enabled));
        log.debug("DB validation-rule: inserted rule id={} name={}", id, name);
    }

    @Override
    public int setEnabled(String id, boolean enabled) {
        return jdbi.withExtension(ValidationRuleDao.class, dao ->
                dao.setEnabled(id, enabled));
    }

    @Override
    public int deleteById(String id) {
        return jdbi.withExtension(ValidationRuleDao.class, dao ->
                dao.deleteById(id));
    }

    // ── Converter ─────────────────────────────────────────────────────────────

    private ValidationRuleEntry toEntry(ValidationRuleRow row) {
        return new ValidationRuleEntry(
                row.getId(), row.getName(), row.getPattern(), row.getTarget(),
                row.isEnabled(), row.getCreatedAt(), row.getUpdatedAt());
    }
}
