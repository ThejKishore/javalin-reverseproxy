/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.db;

import org.example.gateway.config.DataSourceConfig;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Bootstraps the database: creates the {@link DataSource}, runs Flyway migrations,
 * and exposes a configured {@link Jdbi} instance.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final DataSource dataSource;
    private final Jdbi jdbi;

    public DatabaseManager(DataSourceConfig config) {
        this.dataSource = createDataSource(config);
        runMigrations();
        this.jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
        log.info("Database initialised (type={})", config.getType());
    }

    public Jdbi getJdbi() { return jdbi; }
    public DataSource getDataSource() { return dataSource; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private DataSource createDataSource(DataSourceConfig cfg) {
        if ("postgres".equalsIgnoreCase(cfg.getType())) {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setURL(cfg.getUrl());
            ds.setUser(cfg.getUsername());
            ds.setPassword(cfg.getPassword());
            return ds;
        }
        // Default: H2
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(cfg.getUrl());
        ds.setUser(cfg.getUsername());
        ds.setPassword(cfg.getPassword());
        return ds;
    }

    private void runMigrations() {
        log.info("Running Flyway migrations…");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}

