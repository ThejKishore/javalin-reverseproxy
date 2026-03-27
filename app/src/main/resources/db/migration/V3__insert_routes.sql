-- V3: Insert example routes into the gateway

INSERT INTO routes (id, name, enabled, config_json) VALUES
(
    'example-service',
    'Example Service',
    true,
    '{
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
        "audit-store": "database"
    }'
),
(
    'backend-lb',
    'Load-Balanced Backend',
    false,
    '{
        "id": "backend-lb",
        "name": "Load-Balanced Backend",
        "path-pattern": "/api/backend",
        "routing-type": "PATH",
        "strip-prefix": "/api/backend",
        "enabled": false,
        "timeout-ms": 3000,
        "load-balancer-type": "ROUND_ROBIN",
        "targets": [
            {
                "url": "http://backend-1:8080",
                "weight": 2
            },
            {
                "url": "http://backend-2:8080",
                "weight": 1
            }
        ],
        "circuit-breaker-policy": {
            "enabled": true,
            "failure-rate-threshold": 50,
            "wait-duration-seconds": 30,
            "sliding-window-size": 10
        },
        "rate-limit-policy": {
            "enabled": true,
            "requests-per-second": 200,
            "burst": 50
        },
        "audit-enabled": true,
        "audit-store": "database"
    }'
),
(
    'canary-service',
    'Canary Service',
    false,
    '{
        "id": "canary-service",
        "name": "Canary Service",
        "path-pattern": "/api/canary",
        "routing-type": "HEADER",
        "header-match-name": "X-Canary",
        "header-match-value": "true",
        "enabled": false,
        "timeout-ms": 5000,
        "load-balancer-type": "RANDOM",
        "targets": [
            {
                "url": "http://canary-service:8080",
                "weight": 1
            }
        ]
    }'
),
(
    'traffic-split',
    'Traffic Split (90/10)',
    false,
    '{
        "id": "traffic-split",
        "name": "Traffic Split (90/10)",
        "path-pattern": "/api/split",
        "routing-type": "TRAFFIC_SPLIT",
        "enabled": false,
        "timeout-ms": 5000,
        "load-balancer-type": "WEIGHTED",
        "targets": [
            {
                "url": "http://stable-service:8080",
                "weight": 9
            },
            {
                "url": "http://beta-service:8080",
                "weight": 1
            }
        ]
    }'
),
(
    'regex-route',
    'Regex Route',
    false,
    '{
        "id": "regex-route",
        "name": "Regex Route",
        "path-pattern": "/api/v[0-9]+/.*",
        "routing-type": "REGEX",
        "enabled": false,
        "timeout-ms": 5000,
        "load-balancer-type": "ROUND_ROBIN",
        "targets": [
            {
                "url": "http://versioned-service:8080",
                "weight": 1
            }
        ],
        "cache-policy": {
            "enabled": true,
            "ttl-seconds": 60,
            "cache-key-strategy": "METHOD_PATH_QUERY"
        }
    }'
);

