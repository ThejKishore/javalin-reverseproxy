/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.storage.validationrule;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import org.example.gateway.routes.dao.ValidationRuleEntry;
import org.example.gateway.storage.azuretable.AzureTableClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link ValidationRuleStorageProvider} backed by Azure Table Storage.
 *
 * <p>Entity mapping:
 * <ul>
 *   <li>PartitionKey = {@code "validation"} — all rules share one partition.</li>
 *   <li>RowKey       = rule {@code id}.</li>
 *   <li>Properties   = {@code name}, {@code pattern}, {@code target}, {@code enabled},
 *                      {@code createdAt}, {@code updatedAt}.</li>
 * </ul>
 *
 * <p>Activated when {@code gateway.config-source = azure-table}.
 */
public class AzureTableValidationRuleStorageProvider implements ValidationRuleStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureTableValidationRuleStorageProvider.class);
    private static final String PARTITION = "validation";

    private static final String COL_NAME       = "name";
    private static final String COL_PATTERN    = "pattern";
    private static final String COL_TARGET     = "target";
    private static final String COL_ENABLED    = "enabled";
    private static final String COL_CREATED_AT = "createdAt";
    private static final String COL_UPDATED_AT = "updatedAt";

    private final TableClient tableClient;

    public AzureTableValidationRuleStorageProvider(AzureTableClientFactory factory) {
        this.tableClient = factory.validationClient();
        log.info("AzureTableValidationRuleStorageProvider initialised (table={})", tableClient.getTableName());
    }

    @Override
    public List<ValidationRuleEntry> findAll() {
        List<ValidationRuleEntry> results = new ArrayList<>();
        tableClient.listEntities().forEach(entity -> results.add(toEntry(entity)));
        return results;
    }

    @Override
    public List<ValidationRuleEntry> findAllEnabled() {
        String filter = String.format("%s eq true", COL_ENABLED);
        ListEntitiesOptions options = new ListEntitiesOptions().setFilter(filter);
        List<ValidationRuleEntry> results = new ArrayList<>();
        tableClient.listEntities(options, null, null).forEach(entity -> results.add(toEntry(entity)));
        return results;
    }

    @Override
    public void insert(String id, String name, String pattern, String target, boolean enabled) {
        String rowKey = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        Instant now = Instant.now();
        TableEntity entity = new TableEntity(PARTITION, rowKey);
        entity.addProperty(COL_NAME, name);
        entity.addProperty(COL_PATTERN, pattern);
        entity.addProperty(COL_TARGET, target);
        entity.addProperty(COL_ENABLED, enabled);
        entity.addProperty(COL_CREATED_AT, now.toString());
        entity.addProperty(COL_UPDATED_AT, now.toString());
        tableClient.createEntity(entity);
        log.debug("Azure Table validation-rule: inserted id={} name={}", rowKey, name);
    }

    @Override
    public int setEnabled(String id, boolean enabled) {
        try {
            TableEntity entity = tableClient.getEntity(PARTITION, id);
            entity.addProperty(COL_ENABLED, enabled);
            entity.addProperty(COL_UPDATED_AT, Instant.now().toString());
            tableClient.updateEntity(entity);
            log.debug("Azure Table validation-rule: setEnabled id={} enabled={}", id, enabled);
            return 1;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return 0;
            throw e;
        }
    }

    @Override
    public int deleteById(String id) {
        try {
            tableClient.deleteEntity(PARTITION, id);
            log.debug("Azure Table validation-rule: deleted id={}", id);
            return 1;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 404) return 0;
            throw e;
        }
    }

    // ── Converter ─────────────────────────────────────────────────────────────

    private ValidationRuleEntry toEntry(TableEntity entity) {
        return new ValidationRuleEntry(
                entity.getRowKey(),
                str(entity, COL_NAME),
                str(entity, COL_PATTERN),
                str(entity, COL_TARGET),
                Boolean.TRUE.equals(entity.getProperty(COL_ENABLED)),
                instantProp(entity, COL_CREATED_AT),
                instantProp(entity, COL_UPDATED_AT));
    }

    // ── Property helpers ──────────────────────────────────────────────────────

    private static String str(TableEntity e, String key) {
        Object v = e.getProperty(key);
        return v != null ? v.toString() : null;
    }

    private static Instant instantProp(TableEntity e, String key) {
        Object v = e.getProperty(key);
        if (v == null) return null;
        if (v instanceof Instant i) return i;
        try { return Instant.parse(v.toString()); } catch (Exception ex) { return null; }
    }
}
