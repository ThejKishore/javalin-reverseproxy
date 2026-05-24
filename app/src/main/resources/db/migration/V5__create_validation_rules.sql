-- V5: HTTP Input Validation Rules
-- Stores ESAPI blacklist regex patterns for the HttpValidationFilter.
-- Patterns can be updated at runtime via the Admin API without restarting.

CREATE TABLE IF NOT EXISTS validation_rules (
    id          VARCHAR(36)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    pattern     TEXT          NOT NULL,
    target      VARCHAR(32)   NOT NULL DEFAULT 'ALL',   -- QUERY_PARAM | HEADER | COOKIE | BODY | ALL
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_validation_rules PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_validation_rules_enabled ON validation_rules (enabled);
CREATE INDEX IF NOT EXISTS idx_validation_rules_target  ON validation_rules (target);

-- Seed: common blacklist patterns (portable idempotent inserts)
INSERT INTO validation_rules (id, name, pattern, target, enabled)
SELECT 'vr-xss-01', 'XSS script tag', '(?i)<script[^>]*>.*?</script>', 'ALL', TRUE
WHERE NOT EXISTS (SELECT 1 FROM validation_rules WHERE id = 'vr-xss-01');

INSERT INTO validation_rules (id, name, pattern, target, enabled)
SELECT 'vr-xss-02', 'XSS event handler', '(?i)on\w+\s*=', 'ALL', TRUE
WHERE NOT EXISTS (SELECT 1 FROM validation_rules WHERE id = 'vr-xss-02');

INSERT INTO validation_rules (id, name, pattern, target, enabled)
SELECT 'vr-sqli-01', 'SQL injection keyword', '(?i)(union\s+select|drop\s+table|insert\s+into|delete\s+from)', 'QUERY_PARAM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM validation_rules WHERE id = 'vr-sqli-01');

INSERT INTO validation_rules (id, name, pattern, target, enabled)
SELECT 'vr-sqli-02', 'SQL comment injection', '(--|;--|/\*)', 'QUERY_PARAM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM validation_rules WHERE id = 'vr-sqli-02');

INSERT INTO validation_rules (id, name, pattern, target, enabled)
SELECT 'vr-pt-01', 'Path traversal', '(\.\./|\.\.\\\\)', 'ALL', TRUE
WHERE NOT EXISTS (SELECT 1 FROM validation_rules WHERE id = 'vr-pt-01');

