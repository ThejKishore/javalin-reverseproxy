-- V6: Add JWT authentication policy to example-service route

UPDATE routes
SET config_json = '{
    "id": "example-service",
    "name": "Example Service",
    "path-pattern": "/api/example",
    "routing-type": "PATH",
    "strip-prefix": "/api/example",
    "enabled": true,
    "timeout-ms": 5000,
    "load-balancer-type": "ROUND_ROBIN",
    "targets": [
        {
            "url": "https://jsonplaceholder.typicode.com",
            "weight": 1
        }
    ],
    "rate-limit-policy": {
        "enabled": false,
        "requests-per-second": 100,
        "burst": 20,
        "timeout-duration-ms": 0
    },
    "circuit-breaker-policy": {
        "enabled": false,
        "failure-rate-threshold": 50,
        "wait-duration-seconds": 60,
        "sliding-window-size": 10
    },
    "cache-policy": {
        "enabled": false,
        "ttl-seconds": 300,
        "cache-key-strategy": "METHOD_PATH_QUERY"
    },
    "header-rules": {
        "add-request": {
            "X-Gateway": "javalin-gateway",
            "X-Gateway-Version": "1.0.0"
        },
        "exclude-request": [
            "Cookie"
        ],
        "add-response": {
            "X-Powered-By": "javalin-gateway"
        },
        "exclude-response": [
            "Server"
        ],
        "dedupe-response-headers": [
            "Access-Control-Allow-Origin"
        ]
    },
    "auth-forward-headers": [
        "Authorization",
        "X-API-Key"
    ],
    "audit-enabled": false,
    "audit-store": "database",
    "csrf-policy": {
        "enabled": false,
        "token-ttl-seconds": 3600,
        "bind-to": "IP",
        "exclude-paths": []
    },
    "csp-policy": {
        "enabled": false,
        "policy": "default-src ''self''",
        "report-only": false,
        "exclude-paths": []
    },
    "http-validation-policy": {
        "enabled": false,
        "validate-query-params": true,
        "validate-headers": true,
        "validate-cookies": true,
        "validate-body": false,
        "exclude-paths": []
    }
}',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'example-service';

