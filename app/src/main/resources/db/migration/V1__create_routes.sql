-- V1: Core routes table
-- Routes are stored as a JSON blob in config_json for maximum flexibility.

CREATE TABLE IF NOT EXISTS routes (
    id          VARCHAR(36)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    config_json TEXT          NOT NULL,
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_routes PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_routes_enabled ON routes (enabled);

