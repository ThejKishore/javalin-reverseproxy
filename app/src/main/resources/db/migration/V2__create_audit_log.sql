-- V2: Audit log table

CREATE TABLE IF NOT EXISTS audit_logs (
    id           VARCHAR(36)   NOT NULL,
    route_id     VARCHAR(36),
    route_name   VARCHAR(255),
    request_id   VARCHAR(255),
    trace_id     VARCHAR(255),
    http_method  VARCHAR(20)   NOT NULL,
    request_path VARCHAR(2048) NOT NULL,
    upstream_url VARCHAR(2048),
    status_code  INTEGER,
    duration_ms  BIGINT,
    client_ip    VARCHAR(100),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_route_id    ON audit_logs (route_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at  ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id  ON audit_logs (request_id);

