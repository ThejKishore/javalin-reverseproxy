-- V4: Admin change-log table (tracks create/update/delete/enable/disable/reload)

CREATE TABLE IF NOT EXISTS change_logs (
    id          VARCHAR(36)   NOT NULL,
    action      VARCHAR(50)   NOT NULL,
    route_id    VARCHAR(36),
    route_name  VARCHAR(255),
    details     VARCHAR(4000),
    performed_by VARCHAR(100),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_change_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_change_logs_created_at ON change_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_change_logs_route_id   ON change_logs (route_id);
CREATE INDEX IF NOT EXISTS idx_change_logs_action     ON change_logs (action);

