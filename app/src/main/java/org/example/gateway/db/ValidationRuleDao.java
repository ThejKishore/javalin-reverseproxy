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
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * JDBI SqlObject DAO for the {@code validation_rules} table.
 */
@RegisterBeanMapper(ValidationRuleRow.class)
public interface ValidationRuleDao {

    @SqlQuery("SELECT id, name, pattern, target, enabled, created_at, updated_at " +
              "FROM validation_rules WHERE enabled = TRUE ORDER BY created_at")
    List<ValidationRuleRow> findAllEnabled();

    @SqlQuery("SELECT id, name, pattern, target, enabled, created_at, updated_at " +
              "FROM validation_rules ORDER BY created_at")
    List<ValidationRuleRow> findAll();

    @SqlUpdate("INSERT INTO validation_rules (id, name, pattern, target, enabled, created_at, updated_at) " +
               "VALUES (:id, :name, :pattern, :target, :enabled, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    void insert(@Bind("id") String id,
                @Bind("name") String name,
                @Bind("pattern") String pattern,
                @Bind("target") String target,
                @Bind("enabled") boolean enabled);

    @SqlUpdate("UPDATE validation_rules SET enabled = :enabled, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    int setEnabled(@Bind("id") String id, @Bind("enabled") boolean enabled);

    @SqlUpdate("DELETE FROM validation_rules WHERE id = :id")
    int deleteById(@Bind("id") String id);
}

